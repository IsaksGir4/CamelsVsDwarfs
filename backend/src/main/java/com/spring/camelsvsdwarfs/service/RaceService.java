package com.spring.camelsvsdwarfs.service;

import com.spring.camelsvsdwarfs.dto.RaceCreateDTO;
import com.spring.camelsvsdwarfs.dto.RaceResponseDTO;
import com.spring.camelsvsdwarfs.dto.RaceStatusUpdateDTO;
import com.spring.camelsvsdwarfs.dto.RaceUpdateDTO;
import com.spring.camelsvsdwarfs.entity.Race;
import com.spring.camelsvsdwarfs.entity.RaceStatus;
import com.spring.camelsvsdwarfs.entity.RaceType;
import com.spring.camelsvsdwarfs.entity.RegistrationStatus;
import com.spring.camelsvsdwarfs.entity.User;
import com.spring.camelsvsdwarfs.exception.ConflictException;
import com.spring.camelsvsdwarfs.exception.ResourceNotFoundException;
import com.spring.camelsvsdwarfs.repository.RaceRepository;
import com.spring.camelsvsdwarfs.repository.RegisterPlayerRepository;
import com.spring.camelsvsdwarfs.repository.specification.RaceSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RaceService {

    /**
     * Maquina de estados de la carrera.
     * COMPLETED y CANCELLED son estados finales: una carrera completada
     * no puede volver a DRAFT, aunque el camello pida la revancha.
     */
    private static final Map<RaceStatus, Set<RaceStatus>> ALLOWED_TRANSITIONS = Map.of(
            RaceStatus.DRAFT, EnumSet.of(RaceStatus.OPEN_FOR_REGISTRATION, RaceStatus.CANCELLED),
            RaceStatus.OPEN_FOR_REGISTRATION, EnumSet.of(RaceStatus.CLOSED_FOR_REGISTRATION, RaceStatus.CANCELLED),
            RaceStatus.CLOSED_FOR_REGISTRATION, EnumSet.of(RaceStatus.IN_PROGRESS,
                    RaceStatus.OPEN_FOR_REGISTRATION, RaceStatus.CANCELLED),
            RaceStatus.IN_PROGRESS, EnumSet.of(RaceStatus.COMPLETED, RaceStatus.CANCELLED),
            RaceStatus.COMPLETED, EnumSet.noneOf(RaceStatus.class),
            RaceStatus.CANCELLED, EnumSet.noneOf(RaceStatus.class)
    );

    private static final Set<RaceStatus> NON_EDITABLE_STATUSES =
            EnumSet.of(RaceStatus.IN_PROGRESS, RaceStatus.COMPLETED, RaceStatus.CANCELLED);

    private static final Set<RegistrationStatus> ACTIVE_REGISTRATIONS =
            EnumSet.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED);

    private final RaceRepository raceRepository;
    private final RegisterPlayerRepository registerPlayerRepository;
    private final UserSyncService userSyncService;

    @Transactional
    public RaceResponseDTO create(RaceCreateDTO dto, Jwt jwt) {
        validateSchedule(dto.programationDate(), dto.programationHour(), dto.registrationDeadline());

        User organizer = userSyncService.findOrCreateUser(jwt);

        Race race = Race.builder()
                .organizer(organizer)
                .raceName(dto.raceName())
                .description(dto.description())
                .programationDate(dto.programationDate())
                .programationHour(dto.programationHour())
                .ubicationStart(dto.ubicationStart())
                .ubicationFinish(dto.ubicationFinish())
                .distanceMeters(dto.distanceMeters())
                .maxPlayers(dto.maxPlayers())
                .raceType(dto.raceType())
                .raceStatus(RaceStatus.DRAFT)
                .registrationDeadline(dto.registrationDeadline())
                .build();

        Race saved = raceRepository.save(race);
        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public RaceResponseDTO findById(UUID id) {
        return toResponseDTO(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public Page<RaceResponseDTO> findAll(RaceStatus status, RaceType type, String name,
                                         LocalDate scheduledAfter, Pageable pageable) {
        Specification<Race> spec = Specification
                .where(RaceSpecification.hasStatus(status))
                .and(RaceSpecification.hasType(type))
                .and(RaceSpecification.nameContains(name))
                .and(RaceSpecification.scheduledAfter(scheduledAfter));

        return raceRepository.findAll(spec, pageable)
                .map(this::toResponseDTO);
    }

    @Transactional
    public RaceResponseDTO update(UUID id, RaceUpdateDTO dto) {
        Race race = findEntityById(id);

        if (NON_EDITABLE_STATUSES.contains(race.getRaceStatus())) {
            throw new ConflictException(
                    "No se puede editar una carrera en estado " + race.getRaceStatus());
        }

        // Solo se revalida el calendario si cambio; asi se puede editar la descripcion
        // de una carrera cuya fecha limite ya paso sin que el sistema lo impida.
        boolean scheduleChanged = !race.getProgramationDate().equals(dto.programationDate())
                || !race.getProgramationHour().equals(dto.programationHour())
                || !race.getRegistrationDeadline().equals(dto.registrationDeadline());

        if (scheduleChanged) {
            validateSchedule(dto.programationDate(), dto.programationHour(), dto.registrationDeadline());
        }

        long activeRegistrations = registerPlayerRepository.countByRace_IdRaceAndStatusIn(id, ACTIVE_REGISTRATIONS);
        if (dto.maxPlayers() < activeRegistrations) {
            throw new ConflictException("No se puede reducir el maximo a " + dto.maxPlayers()
                    + ": la carrera ya tiene " + activeRegistrations + " inscripciones activas");
        }
        if (activeRegistrations > 0 && race.getRaceType() != dto.raceType()) {
            throw new ConflictException("No se puede cambiar el tipo de carrera cuando ya tiene inscripciones activas");
        }

        race.setRaceName(dto.raceName());
        race.setDescription(dto.description());
        race.setProgramationDate(dto.programationDate());
        race.setProgramationHour(dto.programationHour());
        race.setUbicationStart(dto.ubicationStart());
        race.setUbicationFinish(dto.ubicationFinish());
        race.setDistanceMeters(dto.distanceMeters());
        race.setMaxPlayers(dto.maxPlayers());
        race.setRaceType(dto.raceType());
        race.setRegistrationDeadline(dto.registrationDeadline());

        return toResponseDTO(raceRepository.save(race));
    }

    @Transactional
    public RaceResponseDTO changeStatus(UUID id, RaceStatusUpdateDTO dto) {
        Race race = findEntityById(id);
        RaceStatus current = race.getRaceStatus();
        RaceStatus next = dto.newStatus();

        if (current == next) {
            throw new ConflictException("La carrera ya se encuentra en estado " + next);
        }

        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(next)) {
            throw new ConflictException("Transicion de estado no permitida: " + current + " -> " + next);
        }

        if (next == RaceStatus.OPEN_FOR_REGISTRATION
                && race.getRegistrationDeadline().isBefore(LocalDate.now())) {
            throw new ConflictException(
                    "No se pueden abrir inscripciones: la fecha limite de registro ya paso");
        }

        if (next == RaceStatus.IN_PROGRESS) {
            long approved = registerPlayerRepository.countByRace_IdRaceAndStatus(id, RegistrationStatus.APPROVED);
            if (approved < 2) {
                throw new ConflictException("Se requieren al menos 2 participantes aprobados para iniciar la carrera"
                        + " (actualmente: " + approved + ")");
            }
        }

        // TODO(feature/results): COMPLETED exige resultados oficiales
        //   (standingResultRepository.existsByRace_IdRace(id))

        race.setRaceStatus(next);
        return toResponseDTO(raceRepository.save(race));
    }

    @Transactional
    public void delete(UUID id) {
        Race race = findEntityById(id);

        if (race.getRaceStatus() != RaceStatus.DRAFT) {
            throw new ConflictException(
                    "Solo se pueden eliminar carreras en estado DRAFT. " +
                            "Use CANCELLED para carreras que ya avanzaron de estado.");
        }

        raceRepository.delete(race);
    }

    private void validateSchedule(LocalDate date, LocalTime hour, LocalDate registrationDeadline) {
        LocalDateTime start = LocalDateTime.of(date, hour);
        if (start.isBefore(LocalDateTime.now())) {
            throw new ConflictException("La carrera no puede programarse en el pasado");
        }
        if (!registrationDeadline.isBefore(date)) {
            throw new ConflictException(
                    "La fecha limite de registro debe ser anterior a la fecha de la carrera");
        }
        if (registrationDeadline.isBefore(LocalDate.now())) {
            throw new ConflictException("La fecha limite de registro no puede estar en el pasado");
        }
    }

    private Race findEntityById(UUID id) {
        return raceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrera con ID " + id + " no fue encontrada"));
    }

    private RaceResponseDTO toResponseDTO(Race race) {
        return new RaceResponseDTO(
                race.getIdRace(),
                race.getOrganizer().getIdUser(),
                race.getOrganizer().getUsername(),
                race.getRaceName(),
                race.getDescription(),
                race.getProgramationDate(),
                race.getProgramationHour(),
                race.getUbicationStart(),
                race.getUbicationFinish(),
                race.getDistanceMeters(),
                race.getMaxPlayers(),
                race.getRaceType(),
                race.getRaceStatus(),
                race.getRegistrationDeadline(),
                race.getCreationDate(),
                race.getLastUpdate()
        );
    }
}