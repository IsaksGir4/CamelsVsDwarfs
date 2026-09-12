package com.spring.camelsvsdwarfs.repository;

import com.spring.camelsvsdwarfs.entity.ResultStatus;
import com.spring.camelsvsdwarfs.entity.StandingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StandingResultRepository extends JpaRepository<StandingResult, UUID> {

    List<StandingResult> findByRace_IdRaceOrderByEndPositionAsc(UUID raceId);

    List<StandingResult> findByPlayer_IdPlayer(UUID playerId);

    List<StandingResult> findByTeam_IdTeam(UUID teamId);

    Optional<StandingResult> findByRace_IdRaceAndPlayer_IdPlayer(UUID raceId, UUID playerId);

    Optional<StandingResult> findByRace_IdRaceAndTeam_IdTeam(UUID raceId, UUID teamId);

    boolean existsByRace_IdRaceAndPlayer_IdPlayer(UUID raceId, UUID playerId);

    boolean existsByRace_IdRaceAndTeam_IdTeam(UUID raceId, UUID teamId);

    boolean existsByRace_IdRaceAndEndPositionAndStatusResult(
            UUID raceId, Integer endPosition, ResultStatus statusResult);

    boolean existsByRace_IdRace(UUID raceId);

    boolean existsByPlayer_IdPlayer(UUID playerId);

    boolean existsByTeam_IdTeam(UUID teamId);

    @Query("""
            SELECT sr.player.idPlayer AS participantId, SUM(
                CASE sr.endPosition
                    WHEN 1 THEN 10 WHEN 2 THEN 7 WHEN 3 THEN 5 WHEN 4 THEN 3 WHEN 5 THEN 1
                    ELSE 0
                END) AS totalPoints
            FROM StandingResult sr
            WHERE sr.player IS NOT NULL AND sr.statusResult = 'FINISHED'
            GROUP BY sr.player.idPlayer
            """)
    List<Object[]> aggregatePlayerPoints();

    @Query("""
            SELECT sr.team.idTeam AS participantId, SUM(
                CASE sr.endPosition
                    WHEN 1 THEN 10 WHEN 2 THEN 7 WHEN 3 THEN 5 WHEN 4 THEN 3 WHEN 5 THEN 1
                    ELSE 0
                END) AS totalPoints
            FROM StandingResult sr
            WHERE sr.team IS NOT NULL AND sr.statusResult = 'FINISHED'
            GROUP BY sr.team.idTeam
            """)
    List<Object[]> aggregateTeamPoints();
}