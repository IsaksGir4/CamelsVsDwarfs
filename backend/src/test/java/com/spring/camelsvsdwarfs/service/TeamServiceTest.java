package com.spring.camelsvsdwarfs.service;

import com.spring.camelsvsdwarfs.dto.TeamCreateDTO;
import com.spring.camelsvsdwarfs.dto.TeamMemberResponseDTO;
import com.spring.camelsvsdwarfs.dto.TeamResponseDTO;
import com.spring.camelsvsdwarfs.dto.TeamStatusUpdateDTO;
import com.spring.camelsvsdwarfs.dto.TeamUpdateDTO;
import com.spring.camelsvsdwarfs.entity.*;
import com.spring.camelsvsdwarfs.exception.ConflictException;
import com.spring.camelsvsdwarfs.exception.ResourceNotFoundException;
import com.spring.camelsvsdwarfs.repository.PlayerRepository;
import com.spring.camelsvsdwarfs.repository.TeamMemberRepository;
import com.spring.camelsvsdwarfs.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private TeamService teamService;

    private UUID teamId;
    private UUID playerId;
    private Team existingTeam;
    private Player existingPlayer;

    @BeforeEach
    void setUp() {
        teamId = UUID.randomUUID();
        playerId = UUID.randomUUID();

        existingTeam = Team.builder()
                .idTeam(teamId)
                .teamName("The Five Exceptions")
                .description("Equipo de enanos")
                .responsableCoach("Mr. Abandonado")
                .category(TeamCategory.QUARTET)
                .status(TeamStatus.ACTIVE)
                .creationDate(LocalDate.now())
                .victories(0)
                .defeats(0)
                .build();

        existingPlayer = Player.builder()
                .idPlayer(playerId)
                .name("Null Pointer")
                .nickname("NullP")
                .playerType(PlayerType.DWARF)
                .birthDate(LocalDate.of(1990, 3, 3))
                .height(new BigDecimal("1.20"))
                .weight(new BigDecimal("60.00"))
                .placeOfBirth("Medellin")
                .actualState(PlayerState.ACTIVE)
                .registerDate(LocalDate.now())
                .victories(0)
                .defeats(0)
                .racesCompleted(0)
                .build();
    }

    // ---------- CREATE ----------

    @Test
    void create_conNombreUnico_creaEquipoCorrectamente() {
        TeamCreateDTO dto = new TeamCreateDTO(
                "The Five Exceptions", "Equipo de enanos", "Mr. Abandonado", TeamCategory.QUARTET
        );

        when(teamRepository.existsByTeamName("The Five Exceptions")).thenReturn(false);
        when(teamRepository.save(any(Team.class))).thenReturn(existingTeam);
        when(teamMemberRepository.countByTeam_IdTeamAndStatus(any(), eq(TeamMemberStatus.ACTIVE)))
                .thenReturn(0L);

        TeamResponseDTO result = teamService.create(dto);

        assertThat(result.teamName()).isEqualTo("The Five Exceptions");
        assertThat(result.status()).isEqualTo(TeamStatus.ACTIVE);
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    void create_conNombreDuplicado_lanzaConflictException() {
        TeamCreateDTO dto = new TeamCreateDTO(
                "The Five Exceptions", "Otro equipo", "Otro Coach", TeamCategory.TRIO
        );

        when(teamRepository.existsByTeamName("The Five Exceptions")).thenReturn(true);

        assertThatThrownBy(() -> teamService.create(dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("The Five Exceptions");

        verify(teamRepository, never()).save(any());
    }

    // ---------- FIND ----------

    @Test
    void findById_conIdInexistente_lanzaResourceNotFoundException() {
        UUID randomId = UUID.randomUUID();
        when(teamRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.findById(randomId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(randomId.toString());
    }

    // ---------- UPDATE ----------

    @Test
    void update_reduciendoCategoriaPorDebajoDeMiembrosActivos_lanzaConflictException() {
        TeamUpdateDTO dto = new TeamUpdateDTO(
                "The Five Exceptions", "Equipo de enanos", "Mr. Abandonado", TeamCategory.DUO
        );

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(existingTeam));
        when(teamMemberRepository.countByTeam_IdTeamAndStatus(teamId, TeamMemberStatus.ACTIVE))
                .thenReturn(3L);

        assertThatThrownBy(() -> teamService.update(teamId, dto))
                .isInstanceOf(ConflictException.class);

        verify(teamRepository, never()).save(any());
    }

    // ---------- CHANGE STATUS ----------

    @Test
    void changeStatus_alMismoEstadoActual_lanzaConflictException() {
        TeamStatusUpdateDTO dto = new TeamStatusUpdateDTO(TeamStatus.ACTIVE);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(existingTeam));

        assertThatThrownBy(() -> teamService.changeStatus(teamId, dto))
                .isInstanceOf(ConflictException.class);
    }

    // ---------- DELETE ----------

    @Test
    void delete_sinHistorialDeCarreras_eliminaCorrectamente() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(existingTeam));

        teamService.delete(teamId);

        verify(teamRepository).delete(existingTeam);
    }

    @Test
    void delete_conVictoriasRegistradas_lanzaConflictExceptionYNoElimina() {
        existingTeam.setVictories(2);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(existingTeam));

        assertThatThrownBy(() -> teamService.delete(teamId))
                .isInstanceOf(ConflictException.class);

        verify(teamRepository, never()).delete(any(Team.class));
    }

    // ---------- ADD MEMBER ----------

    @Test
    void addMember_conJugadorActivoYEquipoConCupo_agregaCorrectamente() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(existingTeam));
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(existingPlayer));
        when(teamMemberRepository.existsByTeam_IdTeamAndPlayer_IdPlayerAndStatus(
                teamId, playerId, TeamMemberStatus.ACTIVE)).thenReturn(false);
        when(teamMemberRepository.findByPlayer_IdPlayerAndStatus(playerId, TeamMemberStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(teamMemberRepository.countByTeam_IdTeamAndStatus(teamId, TeamMemberStatus.ACTIVE))
                .thenReturn(1L);
        when(teamMemberRepository.save(any(TeamMember.class)))
                .thenAnswer(invocation -> {
                    TeamMember tm = invocation.getArgument(0);
                    tm.setIdTeamMember(UUID.randomUUID());
                    return tm;
                });

        TeamMemberResponseDTO result = teamService.addMember(teamId, playerId);

        assertThat(result.playerId()).isEqualTo(playerId);
        assertThat(result.status()).isEqualTo(TeamMemberStatus.ACTIVE);
        verify(teamMemberRepository).save(any(TeamMember.class));
    }

    @Test
    void addMember_conEquipoLleno_lanzaConflictException() {
        existingTeam.setCategory(TeamCategory.DUO);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(existingTeam));
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(existingPlayer));
        when(teamMemberRepository.existsByTeam_IdTeamAndPlayer_IdPlayerAndStatus(
                teamId, playerId, TeamMemberStatus.ACTIVE)).thenReturn(false);
        when(teamMemberRepository.findByPlayer_IdPlayerAndStatus(playerId, TeamMemberStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(teamMemberRepository.countByTeam_IdTeamAndStatus(teamId, TeamMemberStatus.ACTIVE))
                .thenReturn(2L);

        assertThatThrownBy(() -> teamService.addMember(teamId, playerId))
                .isInstanceOf(ConflictException.class);

        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    void addMember_conJugadorYaEnOtroEquipoActivo_lanzaConflictException() {
        UUID otroTeamId = UUID.randomUUID();
        TeamMember membresiaEnOtroEquipo = TeamMember.builder()
                .idTeamMember(UUID.randomUUID())
                .team(Team.builder().idTeam(otroTeamId).build())
                .player(existingPlayer)
                .joinDate(LocalDate.now())
                .status(TeamMemberStatus.ACTIVE)
                .build();

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(existingTeam));
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(existingPlayer));
        when(teamMemberRepository.existsByTeam_IdTeamAndPlayer_IdPlayerAndStatus(
                teamId, playerId, TeamMemberStatus.ACTIVE)).thenReturn(false);
        when(teamMemberRepository.findByPlayer_IdPlayerAndStatus(playerId, TeamMemberStatus.ACTIVE))
                .thenReturn(Optional.of(membresiaEnOtroEquipo));

        assertThatThrownBy(() -> teamService.addMember(teamId, playerId))
                .isInstanceOf(ConflictException.class);

        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    void addMember_conJugadorNoActivo_lanzaConflictException() {
        existingPlayer.setActualState(PlayerState.SUSPENDED);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(existingTeam));
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(existingPlayer));

        assertThatThrownBy(() -> teamService.addMember(teamId, playerId))
                .isInstanceOf(ConflictException.class);

        verify(teamMemberRepository, never()).save(any());
    }

    // ---------- REMOVE MEMBER ----------

    @Test
    void removeMember_conMembresiaActiva_marcaComoInactivaYRegistraLeaveDate() {
        TeamMember member = TeamMember.builder()
                .idTeamMember(UUID.randomUUID())
                .team(existingTeam)
                .player(existingPlayer)
                .joinDate(LocalDate.now().minusMonths(1))
                .status(TeamMemberStatus.ACTIVE)
                .build();

        when(teamMemberRepository.existsByTeam_IdTeamAndPlayer_IdPlayerAndStatus(
                teamId, playerId, TeamMemberStatus.ACTIVE)).thenReturn(true);
        when(teamMemberRepository.findByPlayer_IdPlayerAndStatus(playerId, TeamMemberStatus.ACTIVE))
                .thenReturn(Optional.of(member));
        when(teamMemberRepository.save(any(TeamMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        teamService.removeMember(teamId, playerId);

        assertThat(member.getStatus()).isEqualTo(TeamMemberStatus.INACTIVE);
        assertThat(member.getLeaveDate()).isEqualTo(LocalDate.now());
        verify(teamMemberRepository).save(member);
    }

    @Test
    void removeMember_sinMembresiaActiva_lanzaResourceNotFoundException() {
        when(teamMemberRepository.existsByTeam_IdTeamAndPlayer_IdPlayerAndStatus(
                teamId, playerId, TeamMemberStatus.ACTIVE)).thenReturn(false);

        assertThatThrownBy(() -> teamService.removeMember(teamId, playerId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(teamMemberRepository, never()).save(any());
    }
}