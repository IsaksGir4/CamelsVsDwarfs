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
import com.spring.camelsvsdwarfs.repository.RegisterPlayerRepository;
import com.spring.camelsvsdwarfs.entity.RegistrationStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RaceServiceTest {

    @Mock
    private RaceRepository raceRepository;

    @Mock
    private RegisterPlayerRepository registerPlayerRepository;

    @Mock
    private UserSyncService userSyncService;

    @InjectMocks
    private RaceService raceService;

    private Jwt jwt;
    private User organizer;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("kc-organizer-id")
                .claim("preferred_username", "organizer")
                .build();

        organizer = User.builder()
                .idUser(UUID.randomUUID())
                .keycloakId("kc-organizer-id")
                .username("organizer")
                .build();
    }

    // ---------- helpers ----------

    private RaceCreateDTO createDTO(LocalDate date, LocalTime hour, LocalDate deadline) {
        return new RaceCreateDTO("Gran Carrera EIA", "Camello vs enanos", date, hour,
                "Zuniga", "Las Palmas", 1000, 6, RaceType.MIXED, deadline);
    }

    private Race race(RaceStatus status) {
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
                .raceType(RaceType.MIXED)
                .raceStatus(status)
                .registrationDeadline(LocalDate.now().plusDays(5))
                .build();
    }

    // ---------- create ----------

    @Test
    void create_validRace_returnsDraftWithOrganizer() {
        when(userSyncService.findOrCreateUser(jwt)).thenReturn(organizer);
        when(raceRepository.save(any(Race.class))).thenAnswer(inv -> inv.getArgument(0));

        RaceResponseDTO result = raceService.create(
                createDTO(LocalDate.now().plusDays(10), LocalTime.of(10, 0), LocalDate.now().plusDays(5)), jwt);

        assertThat(result.raceStatus()).isEqualTo(RaceStatus.DRAFT);
        assertThat(result.organizerUsername()).isEqualTo("organizer");
        verify(raceRepository).save(any(Race.class));
    }

    @Test
    void create_raceInThePast_throwsConflict() {
        RaceCreateDTO dto = createDTO(LocalDate.now().minusDays(1), LocalTime.of(10, 0),
                LocalDate.now().minusDays(3));

        assertThatThrownBy(() -> raceService.create(dto, jwt))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("pasado");
        verify(raceRepository, never()).save(any());
    }

    @Test
    void create_deadlineNotBeforeRaceDate_throwsConflict() {
        LocalDate date = LocalDate.now().plusDays(10);
        RaceCreateDTO dto = createDTO(date, LocalTime.of(10, 0), date);

        assertThatThrownBy(() -> raceService.create(dto, jwt))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("fecha limite");
        verify(raceRepository, never()).save(any());
    }

    // ---------- update ----------

    @Test
    void update_completedRace_throwsConflict() {
        Race completed = race(RaceStatus.COMPLETED);
        when(raceRepository.findById(completed.getIdRace())).thenReturn(Optional.of(completed));

        RaceUpdateDTO dto = new RaceUpdateDTO("Nuevo nombre", null,
                completed.getProgramationDate(), completed.getProgramationHour(),
                "Zuniga", "Las Palmas", 1000, 6, RaceType.MIXED, completed.getRegistrationDeadline());

        assertThatThrownBy(() -> raceService.update(completed.getIdRace(), dto))
                .isInstanceOf(ConflictException.class);
        verify(raceRepository, never()).save(any());
    }

    // ---------- changeStatus ----------

    @Test
    void changeStatus_draftToOpen_succeeds() {
        Race draft = race(RaceStatus.DRAFT);
        when(raceRepository.findById(draft.getIdRace())).thenReturn(Optional.of(draft));
        when(raceRepository.save(any(Race.class))).thenAnswer(inv -> inv.getArgument(0));

        RaceResponseDTO result = raceService.changeStatus(draft.getIdRace(),
                new RaceStatusUpdateDTO(RaceStatus.OPEN_FOR_REGISTRATION));

        assertThat(result.raceStatus()).isEqualTo(RaceStatus.OPEN_FOR_REGISTRATION);
    }

    @Test
    void changeStatus_completedToDraft_throwsConflict() {
        Race completed = race(RaceStatus.COMPLETED);
        when(raceRepository.findById(completed.getIdRace())).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> raceService.changeStatus(completed.getIdRace(),
                new RaceStatusUpdateDTO(RaceStatus.DRAFT)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no permitida");
        verify(raceRepository, never()).save(any());
    }

    @Test
    void changeStatus_draftDirectlyToInProgress_throwsConflict() {
        Race draft = race(RaceStatus.DRAFT);
        when(raceRepository.findById(draft.getIdRace())).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> raceService.changeStatus(draft.getIdRace(),
                new RaceStatusUpdateDTO(RaceStatus.IN_PROGRESS)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void changeStatus_toInProgressWithLessThanTwoApproved_throwsConflict() {
        Race closed = race(RaceStatus.CLOSED_FOR_REGISTRATION);
        when(raceRepository.findById(closed.getIdRace())).thenReturn(Optional.of(closed));
        when(registerPlayerRepository.countByRace_IdRaceAndStatus(closed.getIdRace(), RegistrationStatus.APPROVED))
                .thenReturn(1L);

        assertThatThrownBy(() -> raceService.changeStatus(closed.getIdRace(),
                new RaceStatusUpdateDTO(RaceStatus.IN_PROGRESS)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("al menos 2");
        verify(raceRepository, never()).save(any());
    }

    // ---------- delete / findById ----------

    @Test
    void delete_nonDraftRace_throwsConflict() {
        Race open = race(RaceStatus.OPEN_FOR_REGISTRATION);
        when(raceRepository.findById(open.getIdRace())).thenReturn(Optional.of(open));

        assertThatThrownBy(() -> raceService.delete(open.getIdRace()))
                .isInstanceOf(ConflictException.class);
        verify(raceRepository, never()).delete(any(Race.class));
    }

    @Test
    void findById_missingRace_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(raceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> raceService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}