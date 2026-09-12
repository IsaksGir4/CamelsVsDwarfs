package com.spring.camelsvsdwarfs.service;

import com.spring.camelsvsdwarfs.dto.RegistrationCreateDTO;
import com.spring.camelsvsdwarfs.dto.RegistrationRejectDTO;
import com.spring.camelsvsdwarfs.dto.RegistrationResponseDTO;
import com.spring.camelsvsdwarfs.entity.*;
import com.spring.camelsvsdwarfs.exception.ConflictException;
import com.spring.camelsvsdwarfs.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock private RegisterPlayerRepository registrationRepository;
    @Mock private RaceRepository raceRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private UserSyncService userSyncService;

    @InjectMocks
    private RegistrationService registrationService;

    private Jwt jwt;
    private User organizer;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("token").header("alg", "none").subject("kc-org").build();
        organizer = User.builder().idUser(UUID.randomUUID()).keycloakId("kc-org").username("organizer").build();
    }

    // ---------- helpers ----------

    private Race race(RaceStatus status, RaceType type, LocalDate deadline) {
        return Race.builder()
                .idRace(UUID.randomUUID())
                .organizer(organizer)
                .raceName("Gran Carrera EIA")
                .programationDate(LocalDate.now().plusDays(10))
                .programationHour(LocalTime.of(10, 0))
                .ubicationStart("Zuniga")
                .ubicationFinish("Las Palmas")
                .distanceMeters(1000)
                .maxPlayers(6)
                .raceType(type)
                .raceStatus(status)
                .registrationDeadline(deadline)
                .build();
    }

    private Race openRace(RaceType type) {
        return race(RaceStatus.OPEN_FOR_REGISTRATION, type, LocalDate.now().plusDays(5));
    }

    private Player player(PlayerState state) {
        return Player.builder()
                .idPlayer(UUID.randomUUID())
                .name("Byte")
                .nickname("byte")
                .playerType(PlayerType.CAMEL)
                .actualState(state)
                .build();
    }

    private RegistrationCreateDTO forPlayer(UUID playerId) {
        return new RegistrationCreateDTO(playerId, null, null, null);
    }

    // ---------- create ----------

    @Test
    void create_activePlayerInOpenRace_returnsPending() {
        Race race = openRace(RaceType.MIXED);
        Player byte_ = player(PlayerState.ACTIVE);
        when(raceRepository.findById(race.getIdRace())).thenReturn(Optional.of(race));
        when(playerRepository.findById(byte_.getIdPlayer())).thenReturn(Optional.of(byte_));
        when(userSyncService.findOrCreateUser(jwt)).thenReturn(organizer);
        when(registrationRepository.save(any(RegisterPlayer.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrationResponseDTO result = registrationService.create(race.getIdRace(), forPlayer(byte_.getIdPlayer()), jwt);

        assertThat(result.status()).isEqualTo(RegistrationStatus.PENDING);
        assertThat(result.participantType()).isEqualTo("PLAYER");
        assertThat(result.playerNickname()).isEqualTo("byte");
        assertThat(result.registeredBy()).isEqualTo("organizer");
    }

    @Test
    void create_suspendedPlayer_throwsConflict() {
        Race race = openRace(RaceType.MIXED);
        Player suspended = player(PlayerState.SUSPENDED);
        when(raceRepository.findById(race.getIdRace())).thenReturn(Optional.of(race));
        when(playerRepository.findById(suspended.getIdPlayer())).thenReturn(Optional.of(suspended));

        assertThatThrownBy(() -> registrationService.create(race.getIdRace(), forPlayer(suspended.getIdPlayer()), jwt))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ACTIVE");
        verify(registrationRepository, never()).save(any(RegisterPlayer.class));
    }

    @Test
    void create_duplicatedRegistration_throwsConflict() {
        Race race = openRace(RaceType.MIXED);
        Player byte_ = player(PlayerState.ACTIVE);
        when(raceRepository.findById(race.getIdRace())).thenReturn(Optional.of(race));
        when(playerRepository.findById(byte_.getIdPlayer())).thenReturn(Optional.of(byte_));
        when(registrationRepository.existsByRace_IdRaceAndPlayer_IdPlayerAndStatusIn(
                eq(race.getIdRace()), eq(byte_.getIdPlayer()), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> registrationService.create(race.getIdRace(), forPlayer(byte_.getIdPlayer()), jwt))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ya esta inscrito");
        verify(registrationRepository, never()).save(any(RegisterPlayer.class));
    }

    @Test
    void create_afterDeadline_throwsConflict() {
        Race race = race(RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED, LocalDate.now().minusDays(1));
        when(raceRepository.findById(race.getIdRace())).thenReturn(Optional.of(race));

        assertThatThrownBy(() -> registrationService.create(race.getIdRace(), forPlayer(UUID.randomUUID()), jwt))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("fecha limite");
    }

    @Test
    void create_raceNotOpen_throwsConflict() {
        Race cancelled = race(RaceStatus.CANCELLED, RaceType.MIXED, LocalDate.now().plusDays(5));
        when(raceRepository.findById(cancelled.getIdRace())).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> registrationService.create(cancelled.getIdRace(), forPlayer(UUID.randomUUID()), jwt))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no esta abierta");
    }

    @Test
    void create_playerInTeamRace_throwsConflict() {
        Race teamRace = openRace(RaceType.TEAM);
        when(raceRepository.findById(teamRace.getIdRace())).thenReturn(Optional.of(teamRace));

        assertThatThrownBy(() -> registrationService.create(teamRace.getIdRace(), forPlayer(UUID.randomUUID()), jwt))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("TEAM");
    }

    @Test
    void create_raceAtCapacity_throwsConflict() {
        Race race = openRace(RaceType.MIXED);
        when(raceRepository.findById(race.getIdRace())).thenReturn(Optional.of(race));
        when(registrationRepository.countByRace_IdRaceAndStatusIn(eq(race.getIdRace()), anyCollection()))
                .thenReturn(6L);

        assertThatThrownBy(() -> registrationService.create(race.getIdRace(), forPlayer(UUID.randomUUID()), jwt))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("capacidad");
    }

    @Test
    void create_teamWithoutMembers_throwsConflict() {
        Race race = openRace(RaceType.MIXED);
        Team emptyTeam = Team.builder()
                .idTeam(UUID.randomUUID())
                .teamName("The Five Exceptions")
                .status(TeamStatus.ACTIVE)
                .build();
        when(raceRepository.findById(race.getIdRace())).thenReturn(Optional.of(race));
        when(teamRepository.findById(emptyTeam.getIdTeam())).thenReturn(Optional.of(emptyTeam));

        RegistrationCreateDTO dto = new RegistrationCreateDTO(null, emptyTeam.getIdTeam(), null, null);

        assertThatThrownBy(() -> registrationService.create(race.getIdRace(), dto, jwt))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("al menos un competidor");
    }

    // ---------- approve / reject ----------

    @Test
    void approve_pendingRegistration_assignsFirstStartPosition() {
        Race race = openRace(RaceType.MIXED);
        RegisterPlayer pending = RegisterPlayer.builder()
                .idRegister(UUID.randomUUID())
                .race(race)
                .player(player(PlayerState.ACTIVE))
                .registeringUser(organizer)
                .status(RegistrationStatus.PENDING)
                .build();
        when(registrationRepository.findById(pending.getIdRegister())).thenReturn(Optional.of(pending));
        when(registrationRepository.save(any(RegisterPlayer.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrationResponseDTO result = registrationService.approve(pending.getIdRegister());

        assertThat(result.status()).isEqualTo(RegistrationStatus.APPROVED);
        assertThat(result.startPosition()).isEqualTo(1);
        assertThat(result.assignedLane()).isEqualTo(1);
    }

    @Test
    void reject_pendingRegistration_storesReason() {
        Race race = openRace(RaceType.MIXED);
        RegisterPlayer pending = RegisterPlayer.builder()
                .idRegister(UUID.randomUUID())
                .race(race)
                .player(player(PlayerState.ACTIVE))
                .registeringUser(organizer)
                .status(RegistrationStatus.PENDING)
                .build();
        when(registrationRepository.findById(pending.getIdRegister())).thenReturn(Optional.of(pending));
        when(registrationRepository.save(any(RegisterPlayer.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrationResponseDTO result = registrationService.reject(pending.getIdRegister(),
                new RegistrationRejectDTO("El camello intento inscribirse como enano"));

        assertThat(result.status()).isEqualTo(RegistrationStatus.REJECTED);
        assertThat(result.validationNotes()).contains("camello");
    }
}