package com.spring.camelsvsdwarfs.service;

import com.spring.camelsvsdwarfs.dto.RegistrationCreateDTO;
import com.spring.camelsvsdwarfs.dto.RegistrationRejectDTO;
import com.spring.camelsvsdwarfs.dto.RegistrationResponseDTO;
import com.spring.camelsvsdwarfs.entity.*;
import com.spring.camelsvsdwarfs.exception.ConflictException;
import com.spring.camelsvsdwarfs.exception.ResourceNotFoundException;
import com.spring.camelsvsdwarfs.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    /** Inscripciones que ocupan cupo y bloquean duplicados. */
    private static final Set<RegistrationStatus> ACTIVE_STATUSES =
            EnumSet.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED);

    /** Estados de carrera en los que aun se pueden gestionar inscripciones. */
    private static final Set<RaceStatus> MANAGEABLE_RACE_STATUSES =
            EnumSet.of(RaceStatus.OPEN_FOR_REGISTRATION, RaceStatus.CLOSED_FOR_REGISTRATION);

    private final RegisterPlayerRepository registrationRepository;
    private final RaceRepository raceRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserSyncService userSyncService;

    // ------------------------------------------------------------------ create

    @Transactional
    public RegistrationResponseDTO create(UUID raceId, RegistrationCreateDTO dto, Jwt jwt) {
        Race race = findRace(raceId);
        validateRaceOpenForRegistration(race);

        boolean isPlayer = dto.playerId() != null;
        validateTypeMatch(race.getRaceType(), isPlayer);

        long activeRegistrations = registrationRepository.countByRace_IdRaceAndStatusIn(raceId, ACTIVE_STATUSES);
        if (activeRegistrations >= race.getMaxPlayers()) {
            throw new ConflictException(
                    "La carrera alcanzo su capacidad maxima (" + race.getMaxPlayers() + " participantes)");
        }

        if (dto.startPosition() != null) {
            validateStartPosition(race, dto.startPosition());
        }

        RegisterPlayer.RegisterPlayerBuilder builder = RegisterPlayer.builder()
                .race(race)
                .status(RegistrationStatus.PENDING)
                .registerDate(LocalDateTime.now())
                .startPosition(dto.startPosition())
                .assignedLane(dto.assignedLane());

        if (isPlayer) {
            Player player = findPlayer(dto.playerId());
            validatePlayerEligible(player);
            validatePlayerNotAlreadyInRace(raceId, player);
            builder.player(player);
        } else {
            Team team = findTeam(dto.teamId());
            List<TeamMember> members = validateTeamEligible(team);
            validateTeamNotAlreadyInRace(raceId, team, members);
            builder.team(team);
        }

        builder.registeringUser(userSyncService.findOrCreateUser(jwt));

        return toResponseDTO(registrationRepository.save(builder.build()));
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public List<RegistrationResponseDTO> findByRace(UUID raceId, RegistrationStatus status) {
        findRace(raceId); // valida que la carrera exista -> 404
        List<RegisterPlayer> registrations = status == null
                ? registrationRepository.findByRace_IdRaceOrderByRegisterDateAsc(raceId)
                : registrationRepository.findByRace_IdRaceAndStatusOrderByRegisterDateAsc(raceId, status);
        return registrations.stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public RegistrationResponseDTO findById(UUID id) {
        return toResponseDTO(findEntityById(id));
    }

    // ------------------------------------------------------------------ approve / reject / cancel

    @Transactional
    public RegistrationResponseDTO approve(UUID id) {
        RegisterPlayer registration = findEntityById(id);
        Race race = registration.getRace();

        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new ConflictException(
                    "Solo se pueden aprobar inscripciones PENDING (estado actual: " + registration.getStatus() + ")");
        }
        validateRaceManageable(race);

        // Se revalida la elegibilidad: el competidor pudo lesionarse o suspenderse desde que se inscribio
        if (registration.getPlayer() != null) {
            validatePlayerEligible(registration.getPlayer());
        } else {
            validateTeamEligible(registration.getTeam());
        }

        long approved = registrationRepository.countByRace_IdRaceAndStatus(
                race.getIdRace(), RegistrationStatus.APPROVED);
        if (approved >= race.getMaxPlayers()) {
            throw new ConflictException("La carrera ya tiene el maximo de participantes aprobados");
        }

        if (registration.getStartPosition() == null) {
            registration.setStartPosition(nextFreeStartPosition(race.getIdRace()));
        }
        if (registration.getAssignedLane() == null) {
            registration.setAssignedLane(registration.getStartPosition());
        }

        registration.setStatus(RegistrationStatus.APPROVED);
        return toResponseDTO(registrationRepository.save(registration));
    }

    @Transactional
    public RegistrationResponseDTO reject(UUID id, RegistrationRejectDTO dto) {
        RegisterPlayer registration = findEntityById(id);

        if (!ACTIVE_STATUSES.contains(registration.getStatus())) {
            throw new ConflictException(
                    "Solo se pueden rechazar inscripciones PENDING o APPROVED (estado actual: "
                            + registration.getStatus() + ")");
        }
        validateRaceManageable(registration.getRace());

        registration.setStatus(RegistrationStatus.REJECTED);
        registration.setValidationNotes(dto.reason());
        return toResponseDTO(registrationRepository.save(registration));
    }

    /** DELETE: cancelacion logica, se conserva el historial. */
    @Transactional
    public void cancel(UUID id) {
        RegisterPlayer registration = findEntityById(id);

        if (!ACTIVE_STATUSES.contains(registration.getStatus())) {
            throw new ConflictException(
                    "La inscripcion ya se encuentra en estado " + registration.getStatus());
        }
        validateRaceManageable(registration.getRace());

        registration.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);
    }

    // ------------------------------------------------------------------ reglas

    private void validateRaceOpenForRegistration(Race race) {
        if (race.getRaceStatus() != RaceStatus.OPEN_FOR_REGISTRATION) {
            throw new ConflictException(
                    "La carrera no esta abierta a inscripciones (estado actual: " + race.getRaceStatus() + ")");
        }
        if (LocalDate.now().isAfter(race.getRegistrationDeadline())) {
            throw new ConflictException("La fecha limite de inscripcion ya paso");
        }
    }

    private void validateRaceManageable(Race race) {
        if (!MANAGEABLE_RACE_STATUSES.contains(race.getRaceStatus())) {
            throw new ConflictException(
                    "No se pueden gestionar inscripciones de una carrera en estado " + race.getRaceStatus());
        }
    }

    private void validateTypeMatch(RaceType raceType, boolean isPlayer) {
        if (raceType == RaceType.INDIVIDUAL && !isPlayer) {
            throw new ConflictException("Una carrera INDIVIDUAL solo admite competidores individuales");
        }
        if (raceType == RaceType.TEAM && isPlayer) {
            throw new ConflictException("Una carrera TEAM solo admite equipos");
        }
    }

    private void validatePlayerEligible(Player player) {
        if (player.getActualState() != PlayerState.ACTIVE) {
            throw new ConflictException("Solo competidores ACTIVE pueden inscribirse. '"
                    + player.getNickname() + "' esta en estado " + player.getActualState());
        }
    }

    private void validatePlayerNotAlreadyInRace(UUID raceId, Player player) {
        if (registrationRepository.existsByRace_IdRaceAndPlayer_IdPlayerAndStatusIn(
                raceId, player.getIdPlayer(), ACTIVE_STATUSES)) {
            throw new ConflictException("El competidor '" + player.getNickname() + "' ya esta inscrito en esta carrera");
        }

        // No puede competir como individual y como miembro de equipo en la misma carrera
        teamMemberRepository.findByPlayer_IdPlayerAndStatus(player.getIdPlayer(), TeamMemberStatus.ACTIVE)
                .ifPresent(membership -> {
                    if (registrationRepository.existsByRace_IdRaceAndTeam_IdTeamAndStatusIn(
                            raceId, membership.getTeam().getIdTeam(), ACTIVE_STATUSES)) {
                        throw new ConflictException("El competidor '" + player.getNickname()
                                + "' ya compite en esta carrera como miembro del equipo '"
                                + membership.getTeam().getTeamName() + "'");
                    }
                });
    }

    /** Devuelve los miembros activos para reutilizarlos en la validacion de duplicados. */
    private List<TeamMember> validateTeamEligible(Team team) {
        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw new ConflictException("Solo equipos ACTIVE pueden inscribirse. '"
                    + team.getTeamName() + "' esta en estado " + team.getStatus());
        }

        List<TeamMember> members = teamMemberRepository
                .findByTeam_IdTeamAndStatus(team.getIdTeam(), TeamMemberStatus.ACTIVE);
        if (members.isEmpty()) {
            throw new ConflictException("El equipo '" + team.getTeamName()
                    + "' debe tener al menos un competidor para inscribirse");
        }

        List<String> notEligible = members.stream()
                .map(TeamMember::getPlayer)
                .filter(p -> p.getActualState() != PlayerState.ACTIVE)
                .map(p -> p.getNickname() + " (" + p.getActualState() + ")")
                .toList();
        if (!notEligible.isEmpty()) {
            throw new ConflictException("Todos los miembros deben estar ACTIVE. No elegibles: "
                    + String.join(", ", notEligible));
        }
        return members;
    }

    private void validateTeamNotAlreadyInRace(UUID raceId, Team team, List<TeamMember> members) {
        if (registrationRepository.existsByRace_IdRaceAndTeam_IdTeamAndStatusIn(
                raceId, team.getIdTeam(), ACTIVE_STATUSES)) {
            throw new ConflictException("El equipo '" + team.getTeamName() + "' ya esta inscrito en esta carrera");
        }

        for (TeamMember member : members) {
            Player p = member.getPlayer();
            if (registrationRepository.existsByRace_IdRaceAndPlayer_IdPlayerAndStatusIn(
                    raceId, p.getIdPlayer(), ACTIVE_STATUSES)) {
                throw new ConflictException("El competidor '" + p.getNickname()
                        + "' ya esta inscrito individualmente en esta carrera");
            }
        }
    }

    private void validateStartPosition(Race race, Integer startPosition) {
        if (startPosition > race.getMaxPlayers()) {
            throw new ConflictException("La posicion de salida no puede superar el maximo de participantes ("
                    + race.getMaxPlayers() + ")");
        }
        if (registrationRepository.existsByRace_IdRaceAndStartPositionAndStatusIn(
                race.getIdRace(), startPosition, ACTIVE_STATUSES)) {
            throw new ConflictException("La posicion de salida " + startPosition + " ya esta asignada");
        }
    }

    private int nextFreeStartPosition(UUID raceId) {
        Set<Integer> taken = registrationRepository
                .findByRace_IdRaceAndStatus(raceId, RegistrationStatus.APPROVED).stream()
                .map(RegisterPlayer::getStartPosition)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        int position = 1;
        while (taken.contains(position)) {
            position++;
        }
        return position;
    }

    // ------------------------------------------------------------------ helpers

    private Race findRace(UUID id) {
        return raceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrera con ID " + id + " no fue encontrada"));
    }

    private Player findPlayer(UUID id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Competidor con ID " + id + " no fue encontrado"));
    }

    private Team findTeam(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo con ID " + id + " no fue encontrado"));
    }

    private RegisterPlayer findEntityById(UUID id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripcion con ID " + id + " no fue encontrada"));
    }

    private RegistrationResponseDTO toResponseDTO(RegisterPlayer r) {
        Player player = r.getPlayer();
        Team team = r.getTeam();
        return new RegistrationResponseDTO(
                r.getIdRegister(),
                r.getRace().getIdRace(),
                r.getRace().getRaceName(),
                player != null ? "PLAYER" : "TEAM",
                player != null ? player.getIdPlayer() : null,
                player != null ? player.getNickname() : null,
                team != null ? team.getIdTeam() : null,
                team != null ? team.getTeamName() : null,
                r.getRegisteringUser() != null ? r.getRegisteringUser().getUsername() : null,
                r.getRegisterDate(),
                r.getStatus(),
                r.getAssignedLane(),
                r.getStartPosition(),
                r.getValidationNotes()
        );
    }
}