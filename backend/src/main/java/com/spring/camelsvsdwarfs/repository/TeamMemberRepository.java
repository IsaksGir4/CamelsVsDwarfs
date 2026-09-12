package com.spring.camelsvsdwarfs.repository;

import com.spring.camelsvsdwarfs.entity.TeamMember;
import com.spring.camelsvsdwarfs.entity.TeamMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    List<TeamMember> findByTeam_IdTeamAndStatus(UUID teamId, TeamMemberStatus status);

    //Verifica que un jugador este en un equipo activo
    Optional<TeamMember> findByPlayer_IdPlayerAndStatus(UUID playerId, TeamMemberStatus status);

    //verifica si ya es miembro de ESE equipo especifico
    boolean existsByTeam_IdTeamAndPlayer_IdPlayerAndStatus(UUID teamId,UUID playerId, TeamMemberStatus status);

    //cuenta miembros activos
    long countByTeam_IdTeamAndStatus(UUID teamIdTeam, TeamMemberStatus status);

}
