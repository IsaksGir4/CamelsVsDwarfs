package com.spring.camelsvsdwarfs.service;

import com.spring.camelsvsdwarfs.dto.*;
import com.spring.camelsvsdwarfs.entity.*;
import com.spring.camelsvsdwarfs.exception.ConflictException;
import com.spring.camelsvsdwarfs.exception.ResourceNotFoundException;
import com.spring.camelsvsdwarfs.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResultService {

    private final StandingResultRepository standingResultRepository;
    private final RaceRepository raceRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final RegisterPlayerRepository registerPlayerRepository;
    private final UserSyncService userSyncService;

    @Transactional
    public ResultResponseDTO create(UUID raceId, ResultCreateDTO dto, Jwt jwt) {
        Race race = findRaceById(raceId);

        if (race.getRaceStatus() != RaceStatus.IN_PROGRESS) {
            throw new ConflictException("Solo se pueden registrar resultados en carreras IN_PROGRESS");
        }

        RegisterPlayer registration = findApprovedRegistration(raceId, dto.playerId(), dto.teamId());
        validateNoDuplicateResult(raceId, dto.playerId(), dto.teamId());
        validateResultConsistency(dto.statusResult(), dto.endPosition(), dto.totalTimeMs());

        if (dto.statusResult() == ResultStatus.FINISHED) {
            validateEndPositionAvailable(raceId, dto.endPosition(), null);
        }

        User registeringUser = userSyncService.findOrCreateUser(jwt);

        Player player = dto.playerId() != null ? findPlayerById(dto.playerId()) : null;
        Team team = dto.teamId() != null ? findTeamById(dto.teamId()) : null;

        StandingResult result = StandingResult.builder()
                .race(race)
                .player(player)
                .team(team)
                .user(registeringUser)
                .startPosition(registration.getStartPosition())
                .statusResult(dto.statusResult())
                .totalTimeMs(dto.totalTimeMs())
                .endPosition(dto.endPosition())
                .penalizationTimeMs(dto.penalizationTimeMs() != null ? dto.penalizationTimeMs() : 0)
                .completedType(dto.completedType())
                .notes(dto.notes())
                .build();

        StandingResult saved = standingResultRepository.save(result);
        recalculateStatsForRace(raceId);
        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ResultResponseDTO> findByRace(UUID raceId) {
        findRaceById(raceId);
        return standingResultRepository.findByRace_IdRaceOrderByEndPositionAsc(raceId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResultResponseDTO findById(UUID id) {
        return toResponseDTO(findEntityById(id));
    }

    @Transactional
    public ResultResponseDTO update(UUID id, ResultUpdateDTO dto) {
        StandingResult result = findEntityById(id);
        Race race = result.getRace();
        UUID raceId = result.getRace().getIdRace();

        validateRaceEditable(race);

        validateResultConsistency(dto.statusResult(), dto.endPosition(), dto.totalTimeMs());

        if (dto.statusResult() == ResultStatus.FINISHED) {
            validateEndPositionAvailable(raceId, dto.endPosition(), id);
        }

        result.setStatusResult(dto.statusResult());
        result.setTotalTimeMs(dto.totalTimeMs());
        result.setEndPosition(dto.endPosition());
        result.setPenalizationTimeMs(dto.penalizationTimeMs() != null ? dto.penalizationTimeMs() : 0);
        result.setCompletedType(dto.completedType());
        result.setNotes(dto.notes());

        StandingResult updated = standingResultRepository.save(result);
        recalculateStatsForRace(raceId);
        return toResponseDTO(updated);
    }

    @Transactional(readOnly = true)
    public List<PlayerStandingDTO> getPlayerStandings() {
        return standingResultRepository.aggregatePlayerPoints().stream()
                .map(row -> {
                    UUID playerId = (UUID) row[0];
                    Long points = (Long) row[1];
                    Player player = playerRepository.findById(playerId).orElse(null);
                    if (player == null) return null;
                    return new PlayerStandingDTO(
                            player.getIdPlayer(),
                            player.getName(),
                            player.getNickname(),
                            points.intValue(),
                            player.getVictories(),
                            player.getDefeats(),
                            player.getRacesCompleted()
                    );
                })
                .filter(dto -> dto != null)
                .sorted((a, b) -> b.points() - a.points())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamStandingDTO> getTeamStandings() {
        return standingResultRepository.aggregateTeamPoints().stream()
                .map(row -> {
                    UUID teamId = (UUID) row[0];
                    Long points = (Long) row[1];
                    Team team = teamRepository.findById(teamId).orElse(null);
                    if (team == null) return null;
                    return new TeamStandingDTO(
                            team.getIdTeam(),
                            team.getTeamName(),
                            points.intValue(),
                            team.getVictories(),
                            team.getDefeats()
                    );
                })
                .filter(dto -> dto != null)
                .sorted((a, b) -> b.points() - a.points())
                .toList();
    }

    // ---------- Validaciones ----------

    private RegisterPlayer findApprovedRegistration(UUID raceId, UUID playerId, UUID teamId) {
        Optional<RegisterPlayer> registration = playerId != null
                ? registerPlayerRepository.findByRace_IdRaceAndPlayer_IdPlayerAndStatusIn(
                raceId, playerId, List.of(RegistrationStatus.APPROVED))
                : registerPlayerRepository.findByRace_IdRaceAndTeam_IdTeamAndStatusIn(
                raceId, teamId, List.of(RegistrationStatus.APPROVED));

        return registration.orElseThrow(() -> new ConflictException(
                "El participante no tiene una inscripcion APPROVED en esta carrera"));
    }

    private void validateRaceEditable(Race race) {
        if (race.getRaceStatus() != RaceStatus.IN_PROGRESS
                && race.getRaceStatus() != RaceStatus.COMPLETED) {
            throw new ConflictException(
                    "Solo se pueden modificar resultados de carreras IN_PROGRESS o COMPLETED "
                            + "(estado actual: " + race.getRaceStatus() + ")");
        }
    }


    private void validateNoDuplicateResult(UUID raceId, UUID playerId, UUID teamId) {
        boolean exists = playerId != null
                ? standingResultRepository.existsByRace_IdRaceAndPlayer_IdPlayer(raceId, playerId)
                : standingResultRepository.existsByRace_IdRaceAndTeam_IdTeam(raceId, teamId);
        if (exists) {
            throw new ConflictException("Ya existe un resultado registrado para este participante en esta carrera");
        }
    }

    private void validateResultConsistency(ResultStatus status, Integer endPosition, Integer totalTimeMs) {
        if (status == ResultStatus.FINISHED) {
            if (totalTimeMs == null || totalTimeMs <= 0) {
                throw new ConflictException("Un resultado FINISHED requiere un tiempo total positivo");
            }
            if (endPosition == null || endPosition <= 0) {
                throw new ConflictException("Un resultado FINISHED requiere una posicion final valida");
            }
        }

        if (status == ResultStatus.DISQUALIFIED && endPosition != null && endPosition == 1) {
            throw new ConflictException("Un competidor descalificado no puede ganar la carrera");
        }

        if ((status == ResultStatus.DID_NOT_FINISH || status == ResultStatus.DID_NOT_START)
                && endPosition != null) {
            throw new ConflictException(
                    "Un resultado " + status + " no debe tener posicion final");
        }
    }

    private void validateEndPositionAvailable(UUID raceId, Integer endPosition, UUID excludingResultId) {
        boolean taken = standingResultRepository
                .existsByRace_IdRaceAndEndPositionAndStatusResult(raceId, endPosition, ResultStatus.FINISHED);

        if (taken) {
            // Si es un update sobre el mismo registro que ya tenia esa posicion, no es conflicto real
            Optional<StandingResult> current = excludingResultId != null
                    ? Optional.of(findEntityById(excludingResultId))
                    : Optional.empty();

            boolean isSameRecord = current.isPresent()
                    && current.get().getEndPosition() != null
                    && current.get().getEndPosition().equals(endPosition);

            if (!isSameRecord) {
                throw new ConflictException(
                        "La posicion final " + endPosition + " ya fue asignada a otro participante en esta carrera");
            }
        }
    }

    // ---------- Recalculo de estadisticas ----------

    private void recalculateStatsForRace(UUID raceId) {
        List<StandingResult> results = standingResultRepository.findByRace_IdRaceOrderByEndPositionAsc(raceId);

        for (StandingResult result : results) {
            if (result.getPlayer() != null) {
                recalculatePlayerStats(result.getPlayer().getIdPlayer());
            }
            if (result.getTeam() != null) {
                recalculateTeamStats(result.getTeam().getIdTeam());
            }
        }
    }

    private void recalculatePlayerStats(UUID playerId) {
        List<StandingResult> allResults = standingResultRepository.findByPlayer_IdPlayer(playerId);

        int victories = 0, defeats = 0, completed = 0;
        for (StandingResult r : allResults) {
            if (r.getStatusResult() == ResultStatus.FINISHED) {
                completed++;
                if (r.getEndPosition() != null && r.getEndPosition() == 1) {
                    victories++;
                } else {
                    defeats++;
                }
            }
        }

        Player player = findPlayerById(playerId);
        player.setVictories(victories);
        player.setDefeats(defeats);
        player.setRacesCompleted(completed);
        playerRepository.save(player);
    }

    private void recalculateTeamStats(UUID teamId) {
        List<StandingResult> allResults = standingResultRepository.findByTeam_IdTeam(teamId);

        int victories = 0, defeats = 0;
        for (StandingResult r : allResults) {
            if (r.getStatusResult() == ResultStatus.FINISHED) {
                if (r.getEndPosition() != null && r.getEndPosition() == 1) {
                    victories++;
                } else {
                    defeats++;
                }
            }
        }

        Team team = findTeamById(teamId);
        team.setVictories(victories);
        team.setDefeats(defeats);
        teamRepository.save(team);
    }

    // ---------- Helpers ----------

    private Race findRaceById(UUID id) {
        return raceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrera con ID " + id + " no fue encontrada"));
    }

    private Player findPlayerById(UUID id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Competidor con ID " + id + " no fue encontrado"));
    }

    private Team findTeamById(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo con ID " + id + " no fue encontrado"));
    }

    private StandingResult findEntityById(UUID id) {
        return standingResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resultado con ID " + id + " no fue encontrado"));
    }

    private ResultResponseDTO toResponseDTO(StandingResult r) {
        return new ResultResponseDTO(
                r.getIdStandingResult(),
                r.getRace().getIdRace(),
                r.getPlayer() != null ? r.getPlayer().getIdPlayer() : null,
                r.getPlayer() != null ? r.getPlayer().getNickname() : null,
                r.getTeam() != null ? r.getTeam().getIdTeam() : null,
                r.getTeam() != null ? r.getTeam().getTeamName() : null,
                r.getUser().getIdUser(),
                r.getStartPosition(),
                r.getEndPosition(),
                r.getCompletedType(),
                r.getPenalizationTimeMs(),
                r.getStatusResult(),
                r.getTotalTimeMs(),
                r.getNotes(),
                r.getTimestamp()
        );
    }
}