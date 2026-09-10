package com.spring.camelsvsdwarfs.service;

import com.spring.camelsvsdwarfs.dto.*;
import com.spring.camelsvsdwarfs.entity.*;
import com.spring.camelsvsdwarfs.exception.ConflictException;
import com.spring.camelsvsdwarfs.exception.ResourceNotFoundException;
import com.spring.camelsvsdwarfs.repository.PlayerRepository;
import com.spring.camelsvsdwarfs.repository.TeamMemberRepository;
import com.spring.camelsvsdwarfs.repository.TeamRepository;
import com.spring.camelsvsdwarfs.repository.specification.TeamSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PlayerRepository playerRepository;

    @Transactional
    public TeamResponseDTO create(TeamCreateDTO dto) {
        if (teamRepository.existsByTeamName(dto.teamName())) {
            throw new ConflictException("Ya existe un equipo con el nombre '" + dto.teamName() + "'");
        }

        Team team = Team.builder()
                .teamName(dto.teamName())
                .description(dto.description())
                .responsableCoach(dto.responsableCoach())
                .category(dto.category())
                .status(TeamStatus.ACTIVE)
                .creationDate(LocalDate.now())
                .victories(0)
                .defeats(0)
                .build();

        Team saved = teamRepository.save(team);
        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public TeamResponseDTO findById(UUID id) {
        Team team = findEntityById(id);
        return toResponseDTO(team);
    }

    @Transactional(readOnly = true)
    public Page<TeamResponseDTO> findAll(TeamStatus status, String name, Pageable pageable) {
        Specification<Team> spec = Specification
                .where(TeamSpecification.hasStatus(status))
                .and(TeamSpecification.nameContains(name));

        return teamRepository.findAll(spec, pageable)
                .map(this::toResponseDTO);
    }

    @Transactional
    public TeamResponseDTO update(UUID id, TeamUpdateDTO dto) {
        Team team = findEntityById(id);

        if (!team.getTeamName().equals(dto.teamName())
                && teamRepository.existsByTeamName(dto.teamName())) {
            throw new ConflictException("Ya existe un equipo con el nombre '" + dto.teamName() + "'");
        }

        long currentActiveMembers = teamMemberRepository
                .countByTeam_IdTeamAndStatus(id, TeamMemberStatus.ACTIVE);

        if (currentActiveMembers > dto.category().getMaxMembers()) {
            throw new ConflictException(
                    "No se puede reducir la categoria: el equipo tiene " + currentActiveMembers +
                            " miembros activos, mas que el maximo permitido por " + dto.category());
        }

        team.setTeamName(dto.teamName());
        team.setDescription(dto.description());
        team.setResponsableCoach(dto.responsableCoach());
        team.setCategory(dto.category());

        Team updated = teamRepository.save(team);
        return toResponseDTO(updated);
    }

    @Transactional
    public TeamResponseDTO changeStatus(UUID id, TeamStatusUpdateDTO dto) {
        Team team = findEntityById(id);

        if (team.getStatus() == dto.newState()) {
            throw new ConflictException("El equipo ya se encuentra en estado " + dto.newState());
        }

        team.setStatus(dto.newState());
        Team updated = teamRepository.save(team);
        return toResponseDTO(updated);
    }

    @Transactional
    public void delete(UUID id) {
        Team team = findEntityById(id);

        // TODO(feature/results): proxy temporal mientras no existe StandingResultRepository.
        if ((team.getVictories() != null && team.getVictories() > 0)
                || (team.getDefeats() != null && team.getDefeats() > 0)) {
            throw new ConflictException(
                    "No se puede eliminar un equipo con historial de carreras. " +
                            "Debe ser desactivado (INACTIVE) en su lugar.");
        }

        teamRepository.delete(team);
    }

    @Transactional
    public TeamMemberResponseDTO addMember(UUID teamId, UUID playerId) {
        Team team = findEntityById(teamId);
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Competidor con ID " + playerId + " no fue encontrado"));

        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw new ConflictException("No se pueden agregar miembros a un equipo que no esta ACTIVE");
        }

        if (player.getActualState() != PlayerState.ACTIVE) {
            throw new ConflictException("Solo competidores ACTIVE pueden unirse a un equipo");
        }

        boolean alreadyInThisTeam = teamMemberRepository
                .existsByTeam_IdTeamAndPlayer_IdPlayerAndStatus(teamId, playerId, TeamMemberStatus.ACTIVE);
        if (alreadyInThisTeam) {
            throw new ConflictException("El competidor ya es miembro activo de este equipo");
        }

        teamMemberRepository.findByPlayer_IdPlayerAndStatus(playerId, TeamMemberStatus.ACTIVE)
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "El competidor ya pertenece activamente a otro equipo. " +
                                    "Debe salir de su equipo actual antes de unirse a uno nuevo.");
                });

        long currentActiveMembers = teamMemberRepository
                .countByTeam_IdTeamAndStatus(teamId, TeamMemberStatus.ACTIVE);
        if (currentActiveMembers >= team.getCategory().getMaxMembers()) {
            throw new ConflictException(
                    "El equipo ya alcanzo su maximo de miembros (" + team.getCategory().getMaxMembers() + ")");
        }

        TeamMember member = TeamMember.builder()
                .team(team)
                .player(player)
                .joinDate(LocalDate.now())
                .status(TeamMemberStatus.ACTIVE)
                .build();

        TeamMember saved = teamMemberRepository.save(member);
        return toMemberResponseDTO(saved);
    }

    @Transactional
    public void removeMember(UUID teamId, UUID playerId) {
        boolean isActiveMember = teamMemberRepository
                .existsByTeam_IdTeamAndPlayer_IdPlayerAndStatus(teamId, playerId, TeamMemberStatus.ACTIVE);

        if (!isActiveMember) {
            throw new ResourceNotFoundException("El competidor no es miembro activo de este equipo");
        }

        TeamMember member = teamMemberRepository.findByPlayer_IdPlayerAndStatus(playerId, TeamMemberStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Membresia activa no encontrada"));

        member.setStatus(TeamMemberStatus.INACTIVE);
        member.setLeaveDate(LocalDate.now());
        teamMemberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponseDTO> getActiveMembers(UUID teamId) {
        findEntityById(teamId); // valida que el equipo exista
        return teamMemberRepository.findByTeam_IdTeamAndStatus(teamId, TeamMemberStatus.ACTIVE)
                .stream()
                .map(this::toMemberResponseDTO)
                .toList();
    }

    private Team findEntityById(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo con ID " + id + " no fue encontrado"));
    }

    private TeamResponseDTO toResponseDTO(Team team) {
        long currentMembers = teamMemberRepository
                .countByTeam_IdTeamAndStatus(team.getIdTeam(), TeamMemberStatus.ACTIVE);

        return new TeamResponseDTO(
                team.getIdTeam(),
                team.getTeamName(),
                team.getDescription(),
                team.getCreationDate(),
                team.getStatus(),
                team.getResponsableCoach(),
                team.getCategory(),
                (int) currentMembers,
                team.getVictories(),
                team.getDefeats()
        );
    }

    private TeamMemberResponseDTO toMemberResponseDTO(TeamMember member) {
        return new TeamMemberResponseDTO(
                member.getIdTeamMember(),
                member.getPlayer().getIdPlayer(),
                member.getPlayer().getName(),
                member.getPlayer().getNickname(),
                member.getJoinDate(),
                member.getLeaveDate(),
                member.getStatus()
        );
    }
}