package com.spring.camelsvsdwarfs.service;

import com.spring.camelsvsdwarfs.dto.RaceCreateDTO;
import com.spring.camelsvsdwarfs.dto.RaceResponseDTO;
import com.spring.camelsvsdwarfs.dto.RaceStatusUpdateDTO;
import com.spring.camelsvsdwarfs.dto.RaceUpdateDTO;
import com.spring.camelsvsdwarfs.entity.Race;
import com.spring.camelsvsdwarfs.entity.RaceStatus;
import com.spring.camelsvsdwarfs.entity.RaceType;
import com.spring.camelsvsdwarfs.entity.User;
import com.spring.camelsvsdwarfs.exception.ConflictException;
import com.spring.camelsvsdwarfs.exception.ResourceNotFoundException;
import com.spring.camelsvsdwarfs.repository.RaceRepository;
import com.spring.camelsvsdwarfs.repository.specification.RaceSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RaceService {

    private final RaceRepository raceRepository;
    private final UserSyncService userSyncService;

    @Transactional
    public RaceResponseDTO create(RaceCreateDTO dto, Jwt jwt) {
        validateDeadlineBeforeStart(dto.registrationDeadline(), dto.programationDate());

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
        Race race = findEntityById(id);
        return toResponseDTO(race);
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

        if (race.getRaceStatus() == RaceStatus.COMPLETED) {
            throw new ConflictException("Una carrera completada no puede ser editada");
        }

        validateDeadlineBeforeStart(dto.registrationDeadline(), dto.programationDate());

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

        Race updated = raceRepository.save(race);
        return toResponseDTO(updated);
    }

    @Transactional
    public RaceResponseDTO changeStatus(UUID id, RaceStatusUpdateDTO dto) {
        Race race = findEntityById(id);
        RaceStatus current = race.getRaceStatus();
        RaceStatus next = dto.newStatus();

        if (current == next) {
            throw new ConflictException("La carrera ya se encuentra en estado " + next);
        }

        if (current == RaceStatus.COMPLETED) {
            throw new ConflictException("Una carrera completada no puede cambiar de estado");
        }

        if (current == RaceStatus.CANCELLED) {
            throw new ConflictException("Una carrera cancelada no puede cambiar de estado");
        }

        race.setRaceStatus(next);
        Race updated = raceRepository.save(race);
        return toResponseDTO(updated);
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

    private void validateDeadlineBeforeStart(LocalDate registrationDeadline, LocalDate programationDate) {
        if (!registrationDeadline.isBefore(programationDate)) {
            throw new ConflictException(
                    "La fecha limite de registro debe ser anterior a la fecha de la carrera");
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