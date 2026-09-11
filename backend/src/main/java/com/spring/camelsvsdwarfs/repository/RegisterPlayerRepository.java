package com.spring.camelsvsdwarfs.repository;

import com.spring.camelsvsdwarfs.entity.RegisterPlayer;
import com.spring.camelsvsdwarfs.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface RegisterPlayerRepository extends JpaRepository<RegisterPlayer, UUID> {

    List<RegisterPlayer> findByRace_IdRaceOrderByRegisterDateAsc(UUID raceId);

    List<RegisterPlayer> findByRace_IdRaceAndStatusOrderByRegisterDateAsc(UUID raceId, RegistrationStatus status);

    List<RegisterPlayer> findByRace_IdRaceAndStatus(UUID raceId, RegistrationStatus status);

    // Evita inscripciones duplicadas (solo cuentan PENDING y APPROVED)
    boolean existsByRace_IdRaceAndPlayer_IdPlayerAndStatusIn(UUID raceId, UUID playerId,
                                                             Collection<RegistrationStatus> statuses);

    boolean existsByRace_IdRaceAndTeam_IdTeamAndStatusIn(UUID raceId, UUID teamId,
                                                         Collection<RegistrationStatus> statuses);

    // Posiciones de salida no duplicadas
    boolean existsByRace_IdRaceAndStartPositionAndStatusIn(UUID raceId, Integer startPosition,
                                                           Collection<RegistrationStatus> statuses);

    // Capacidad y minimo de participantes para iniciar
    long countByRace_IdRaceAndStatusIn(UUID raceId, Collection<RegistrationStatus> statuses);

    long countByRace_IdRaceAndStatus(UUID raceId, RegistrationStatus status);
}