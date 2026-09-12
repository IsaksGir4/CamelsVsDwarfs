package com.spring.camelsvsdwarfs.service;

import com.spring.camelsvsdwarfs.dto.PlayerStandingDTO;
import com.spring.camelsvsdwarfs.dto.ResultCreateDTO;
import com.spring.camelsvsdwarfs.dto.ResultResponseDTO;
import com.spring.camelsvsdwarfs.dto.ResultUpdateDTO;
import com.spring.camelsvsdwarfs.dto.TeamStandingDTO;
import com.spring.camelsvsdwarfs.entity.*;
import com.spring.camelsvsdwarfs.exception.ConflictException;
import com.spring.camelsvsdwarfs.repository.PlayerRepository;
import com.spring.camelsvsdwarfs.repository.RaceRepository;
import com.spring.camelsvsdwarfs.repository.RegisterPlayerRepository;
import com.spring.camelsvsdwarfs.repository.StandingResultRepository;
import com.spring.camelsvsdwarfs.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock
    private StandingResultRepository standingResultRepository;

    @Mock
    private RaceRepository raceRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private RegisterPlayerRepository registerPlayerRepository;

    @Mock
    private UserSyncService userSyncService;

    @InjectMocks
    private ResultService resultService;

    private UUID raceId;
    private UUID playerId;
    private UUID resultId;
    private Race existingRace;
    private Player existingPlayer;
    private User existingUser;
    private RegisterPlayer approvedRegistration;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        raceId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        resultId = UUID.randomUUID();

        existingRace = Race.builder()
                .idRace(raceId)
                .raceName("Carrera de prueba")
                .raceStatus(RaceStatus.IN_PROGRESS)
                .build();

        existingPlayer = Player.builder()
                .idPlayer(playerId)
                .name("Byte")
                .nickname("ByteTheCamel")
                .victories(0)
                .defeats(0)
                .racesCompleted(0)
                .build();

        existingUser = User.builder()
                .idUser(UUID.randomUUID())
                .keycloakId("kc-id")
                .username("organizer")
                .email("organizer@camelsvsdwarfs.test")
                .build();

        approvedRegistration = RegisterPlayer.builder()
                .idRegister(UUID.randomUUID())
                .race(existingRace)
                .player(existingPlayer)
                .status(RegistrationStatus.APPROVED)
                .startPosition(3)
                .build();

        jwt = Jwt.withTokenValue("t").header("alg", "none").subject("kc-id").build();
    }

    // ---------- CREATE ----------

    @Test
    void create_conResultadoValido_creaCorrectamenteYRecalculaStats() {
        ResultCreateDTO dto = new ResultCreateDTO(
                playerId, null, ResultStatus.FINISHED, 60000, 1, 0, "carrera limpia", null
        );

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(existingRace));
        when(registerPlayerRepository.findByRace_IdRaceAndPlayer_IdPlayerAndStatusIn(
                raceId, playerId, List.of(RegistrationStatus.APPROVED)))
                .thenReturn(Optional.of(approvedRegistration));
        when(standingResultRepository.existsByRace_IdRaceAndPlayer_IdPlayer(raceId, playerId))
                .thenReturn(false);
        when(standingResultRepository.existsByRace_IdRaceAndEndPositionAndStatusResult(
                raceId, 1, ResultStatus.FINISHED)).thenReturn(false);
        when(userSyncService.findOrCreateUser(jwt)).thenReturn(existingUser);
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(existingPlayer));
        when(standingResultRepository.save(any(StandingResult.class)))
                .thenAnswer(invocation -> {
                    StandingResult r = invocation.getArgument(0);
                    r.setIdStandingResult(resultId);
                    return r;
                });

        StandingResult savedForRecalc = StandingResult.builder()
                .idStandingResult(resultId)
                .race(existingRace)
                .player(existingPlayer)
                .statusResult(ResultStatus.FINISHED)
                .endPosition(1)
                .totalTimeMs(60000)
                .build();
        when(standingResultRepository.findByRace_IdRaceOrderByEndPositionAsc(raceId))
                .thenReturn(List.of(savedForRecalc));
        when(standingResultRepository.findByPlayer_IdPlayer(playerId))
                .thenReturn(List.of(savedForRecalc));

        ResultResponseDTO result = resultService.create(raceId, dto, jwt);

        assertThat(result.playerNickname()).isEqualTo("ByteTheCamel");
        assertThat(result.startPosition()).isEqualTo(3);
        assertThat(result.statusResult()).isEqualTo(ResultStatus.FINISHED);
        assertThat(existingPlayer.getVictories()).isEqualTo(1);
        assertThat(existingPlayer.getDefeats()).isZero();
        assertThat(existingPlayer.getRacesCompleted()).isEqualTo(1);
        verify(playerRepository).save(existingPlayer);
    }

    @Test
    void create_conCarreraNoEnProgreso_lanzaConflictException() {
        existingRace.setRaceStatus(RaceStatus.OPEN_FOR_REGISTRATION);
        ResultCreateDTO dto = new ResultCreateDTO(
                playerId, null, ResultStatus.FINISHED, 60000, 1, 0, null, null
        );

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(existingRace));

        assertThatThrownBy(() -> resultService.create(raceId, dto, jwt))
                .isInstanceOf(ConflictException.class);

        verify(standingResultRepository, never()).save(any(StandingResult.class));
    }

    @Test
    void create_sinInscripcionAprobada_lanzaConflictException() {
        ResultCreateDTO dto = new ResultCreateDTO(
                playerId, null, ResultStatus.FINISHED, 60000, 1, 0, null, null
        );

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(existingRace));
        when(registerPlayerRepository.findByRace_IdRaceAndPlayer_IdPlayerAndStatusIn(
                raceId, playerId, List.of(RegistrationStatus.APPROVED)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resultService.create(raceId, dto, jwt))
                .isInstanceOf(ConflictException.class);

        verify(standingResultRepository, never()).save(any(StandingResult.class));
    }

    @Test
    void create_conResultadoDuplicadoParaElMismoParticipante_lanzaConflictException() {
        ResultCreateDTO dto = new ResultCreateDTO(
                playerId, null, ResultStatus.FINISHED, 60000, 1, 0, null, null
        );

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(existingRace));
        when(registerPlayerRepository.findByRace_IdRaceAndPlayer_IdPlayerAndStatusIn(
                raceId, playerId, List.of(RegistrationStatus.APPROVED)))
                .thenReturn(Optional.of(approvedRegistration));
        when(standingResultRepository.existsByRace_IdRaceAndPlayer_IdPlayer(raceId, playerId))
                .thenReturn(true);

        assertThatThrownBy(() -> resultService.create(raceId, dto, jwt))
                .isInstanceOf(ConflictException.class);

        verify(standingResultRepository, never()).save(any(StandingResult.class));
    }

    @Test
    void create_conDosGanadoresEnLaMismaCarrera_lanzaConflictException() {
        ResultCreateDTO dto = new ResultCreateDTO(
                playerId, null, ResultStatus.FINISHED, 55000, 1, 0, null, null
        );

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(existingRace));
        when(registerPlayerRepository.findByRace_IdRaceAndPlayer_IdPlayerAndStatusIn(
                raceId, playerId, List.of(RegistrationStatus.APPROVED)))
                .thenReturn(Optional.of(approvedRegistration));
        when(standingResultRepository.existsByRace_IdRaceAndPlayer_IdPlayer(raceId, playerId))
                .thenReturn(false);
        when(standingResultRepository.existsByRace_IdRaceAndEndPositionAndStatusResult(
                raceId, 1, ResultStatus.FINISHED)).thenReturn(true);

        assertThatThrownBy(() -> resultService.create(raceId, dto, jwt))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ya fue asignada");

        verify(standingResultRepository, never()).save(any(StandingResult.class));
    }

    @Test
    void create_conDescalificadoGanador_lanzaConflictException() {
        ResultCreateDTO dto = new ResultCreateDTO(
                playerId, null, ResultStatus.DISQUALIFIED, null, 1, 0, "salio de la pista", null
        );

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(existingRace));
        when(registerPlayerRepository.findByRace_IdRaceAndPlayer_IdPlayerAndStatusIn(
                raceId, playerId, List.of(RegistrationStatus.APPROVED)))
                .thenReturn(Optional.of(approvedRegistration));
        when(standingResultRepository.existsByRace_IdRaceAndPlayer_IdPlayer(raceId, playerId))
                .thenReturn(false);

        assertThatThrownBy(() -> resultService.create(raceId, dto, jwt))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("descalificado");

        verify(standingResultRepository, never()).save(any(StandingResult.class));
    }

    @Test
    void create_conFinishedSinTiempoPositivo_lanzaConflictException() {
        ResultCreateDTO dto = new ResultCreateDTO(
                playerId, null, ResultStatus.FINISHED, 0, 2, 0, null, null
        );

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(existingRace));
        when(registerPlayerRepository.findByRace_IdRaceAndPlayer_IdPlayerAndStatusIn(
                raceId, playerId, List.of(RegistrationStatus.APPROVED)))
                .thenReturn(Optional.of(approvedRegistration));
        when(standingResultRepository.existsByRace_IdRaceAndPlayer_IdPlayer(raceId, playerId))
                .thenReturn(false);

        assertThatThrownBy(() -> resultService.create(raceId, dto, jwt))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("tiempo total positivo");

        verify(standingResultRepository, never()).save(any(StandingResult.class));
    }

    // ---------- UPDATE ----------

    @Test
    void update_conMismaPosicionFinal_recalculaStatsCorrectamente() {
        StandingResult existingResult = StandingResult.builder()
                .idStandingResult(resultId)
                .race(existingRace)
                .player(existingPlayer)
                .user(existingUser)          // <-- faltaba
                .statusResult(ResultStatus.FINISHED)
                .endPosition(1)
                .totalTimeMs(65000)
                .penalizationTimeMs(0)
                .build();

        ResultUpdateDTO dto = new ResultUpdateDTO(
                ResultStatus.FINISHED, 62000, 1, 0, "tiempo corregido", "ajuste de cronometro"
        );

        when(standingResultRepository.findById(resultId)).thenReturn(Optional.of(existingResult));
        when(standingResultRepository.existsByRace_IdRaceAndEndPositionAndStatusResult(
                raceId, 1, ResultStatus.FINISHED)).thenReturn(true);
        when(standingResultRepository.save(any(StandingResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(standingResultRepository.findByRace_IdRaceOrderByEndPositionAsc(raceId))
                .thenReturn(List.of(existingResult));
        when(standingResultRepository.findByPlayer_IdPlayer(playerId))
                .thenReturn(List.of(existingResult));
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(existingPlayer));

        ResultResponseDTO result = resultService.update(resultId, dto);

        assertThat(result.totalTimeMs()).isEqualTo(62000);
        assertThat(existingPlayer.getVictories()).isEqualTo(1);
        assertThat(existingPlayer.getRacesCompleted()).isEqualTo(1);
        verify(playerRepository).save(existingPlayer);
    }

    @Test
    void update_conCarreraCancelada_lanzaConflictException() {
        existingRace.setRaceStatus(RaceStatus.CANCELLED);

        StandingResult existingResult = StandingResult.builder()
                .idStandingResult(resultId)
                .race(existingRace)
                .player(existingPlayer)
                .user(existingUser)
                .statusResult(ResultStatus.FINISHED)
                .endPosition(1)
                .totalTimeMs(65000)
                .penalizationTimeMs(0)
                .build();

        ResultUpdateDTO dto = new ResultUpdateDTO(
                ResultStatus.FINISHED, 62000, 1, 0, "tiempo corregido", null
        );

        when(standingResultRepository.findById(resultId)).thenReturn(Optional.of(existingResult));

        assertThatThrownBy(() -> resultService.update(resultId, dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CANCELLED");

        verify(standingResultRepository, never()).save(any(StandingResult.class));
    }

    // ---------- STANDINGS ----------

    @Test
    void getPlayerStandings_retornaOrdenadosPorPuntosDescendente() {
        UUID player2Id = UUID.randomUUID();
        Player player2 = Player.builder()
                .idPlayer(player2Id)
                .name("Null Pointer")
                .nickname("NullP")
                .victories(2)
                .defeats(0)
                .racesCompleted(2)
                .build();

        when(standingResultRepository.aggregatePlayerPoints()).thenReturn(List.of(
                new Object[]{playerId, 7L},
                new Object[]{player2Id, 20L}
        ));
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(existingPlayer));
        when(playerRepository.findById(player2Id)).thenReturn(Optional.of(player2));

        List<PlayerStandingDTO> standings = resultService.getPlayerStandings();

        assertThat(standings).hasSize(2);
        assertThat(standings.get(0).nickname()).isEqualTo("NullP");
        assertThat(standings.get(0).points()).isEqualTo(20);
        assertThat(standings.get(1).nickname()).isEqualTo("ByteTheCamel");
    }

    @Test
    void getTeamStandings_retornaOrdenadosPorPuntosDescendente() {
        UUID teamId = UUID.randomUUID();
        Team team = Team.builder()
                .idTeam(teamId)
                .teamName("The Five Exceptions")
                .victories(1)
                .defeats(1)
                .build();

        when(standingResultRepository.aggregateTeamPoints()).thenReturn(
                List.<Object[]>of(new Object[]{teamId, 10L})
        );
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

        List<TeamStandingDTO> standings = resultService.getTeamStandings();

        assertThat(standings).hasSize(1);
        assertThat(standings.get(0).teamName()).isEqualTo("The Five Exceptions");
        assertThat(standings.get(0).points()).isEqualTo(10);
    }
}