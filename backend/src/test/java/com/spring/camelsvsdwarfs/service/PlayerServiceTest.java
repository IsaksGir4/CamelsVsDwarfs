package com.spring.camelsvsdwarfs.service;

import com.spring.camelsvsdwarfs.dto.PlayerCreateDTO;
import com.spring.camelsvsdwarfs.dto.PlayerResponseDTO;
import com.spring.camelsvsdwarfs.dto.PlayerStatusUpdateDTO;
import com.spring.camelsvsdwarfs.dto.PlayerUpdateDTO;
import com.spring.camelsvsdwarfs.entity.Player;
import com.spring.camelsvsdwarfs.entity.PlayerState;
import com.spring.camelsvsdwarfs.entity.PlayerType;
import com.spring.camelsvsdwarfs.exception.ConflictException;
import com.spring.camelsvsdwarfs.exception.ResourceNotFoundException;
import com.spring.camelsvsdwarfs.repository.PlayerRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlayerServiceTest {
    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerService playerService;

    private UUID playerId;
    private Player existingPlayer;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        existingPlayer = Player.builder()
                .idPlayer(playerId)
                .name("ByteAmel")
                .nickname("ByteTheCamel")
                .playerType(PlayerType.CAMEL)
                .birthDate(LocalDate.of(2018,5,10))
                .height(new BigDecimal("2.10"))
                .weight(new BigDecimal("450.00"))
                .placeOfBirth("Alto de las Palmas")
                .actualState(PlayerState.ACTIVE)
                .registerDate(LocalDate.now())
                .victories(0)
                .defeats(0)
                .racesCompleted(0)
                .build();
    }

    @Test
    void create_conNicknameUnico_crearCompetidorCorrectamente() {
        PlayerCreateDTO dto = new PlayerCreateDTO(
                "ByteAmel", "ByteTheCamel", PlayerType.CAMEL,
                LocalDate.of(2018,5,10),
                new BigDecimal("2.10"), new BigDecimal("450.00"),
                "Alto de las Palmas"
        );

        when(playerRepository.existsByNickname("ByteTheCamel")).thenReturn(false);
        when(playerRepository.save(any(Player.class))).thenReturn(existingPlayer);

        PlayerResponseDTO result = playerService.create(dto);
        assertThat(result.nickname()).isEqualTo("ByteTheCamel");
        assertThat(result.actualState()).isEqualTo(PlayerState.ACTIVE);
        verify(playerRepository).save(any(Player.class));

    }

    @Test
    void create_conNicknameDuplicado_lanzaConflictException(){
        PlayerCreateDTO dto = new PlayerCreateDTO(
                "Sebas Zapato","ByteTheCamel", PlayerType.CAMEL,
                LocalDate.of(2019,1,1),
                new BigDecimal("2.00"), new BigDecimal("400.00"),
                "Envigado"
        );

        when(playerRepository.existsByNickname("ByteTheCamel")).thenReturn(true);

        assertThatThrownBy(() -> playerService.create(dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ByteTheCamel");

        verify(playerRepository, never()).save(any());

    }

    @Test
    void create_todoCompetidorNuevo_naceEnEstadoActive() {
        PlayerCreateDTO dto = new PlayerCreateDTO(
                "Null Pointer", "NullP", PlayerType.DWARF,
                LocalDate.of(1990, 3, 3),
                new BigDecimal("1.20"), new BigDecimal("60.00"),
                "Medellin"
        );

        when(playerRepository.existsByNickname("NullP")).thenReturn(false);
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlayerResponseDTO result = playerService.create(dto);

        assertThat(result.actualState()).isEqualTo(PlayerState.ACTIVE);
        assertThat(result.registerDate()).isEqualTo(LocalDate.now());
        assertThat(result.victories()).isZero();
        assertThat(result.defeats()).isZero();
        assertThat(result.racesCompleted()).isZero();
    }

    //------------------------FIND --------------------------

    @Test
    void findById_conIdInexistente_lanzaResourceNotFoundException(){
        UUID randomId = UUID.randomUUID();
        when(playerRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.findById(randomId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(randomId.toString());
    }

    @Test
    void findById_conIdExistente_retornaDTOCorrecto() {
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(existingPlayer));

        PlayerResponseDTO result = playerService.findById(playerId);

        assertThat(result.idPlayer()).isEqualTo(playerId);
        assertThat(result.nickname()).isEqualTo("ByteTheCamel");
    }

    //---------------------- UPDATE ---------------------

    @Test
    void update_conNicknameSinCambios_actualizaCorrectamente(){
        PlayerUpdateDTO dto = new PlayerUpdateDTO(
                "Byte Actualizado", "ByteTheCamel", PlayerType.CAMEL,
                LocalDate.of(2018,5,10),
                new BigDecimal("2.15"), new BigDecimal("455.00"),
                "Alto de las palmas"
        );

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(existingPlayer));
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlayerResponseDTO result = playerService.update(playerId, dto);

        assertThat(result.name()).isEqualTo("Byte Actualizado");
        assertThat(result.height()).isEqualByComparingTo("2.15");

    }

    @Test
    void update_conNicknameQueYaUsaOtroCompetidor_lanzaConflictException() {
        PlayerUpdateDTO dto = new PlayerUpdateDTO(
                "ByteAmel", "NicknameDeOtroJugador", PlayerType.CAMEL,
                LocalDate.of(2018,5,10),
                new BigDecimal("2.10"), new BigDecimal("450.00"),
                "Alto de las Palmas"
        );

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(existingPlayer));
        when(playerRepository.existsByNickname("NicknameDeOtroJugador")).thenReturn(true);

        assertThatThrownBy(() -> playerService.update(playerId, dto))
                .isInstanceOf(ConflictException.class);

        verify(playerRepository, never()).save(any());
    }


    // ---------- CHANGE STATUS ----------

    @Test
    void changeStatus_alMismoEstadoActual_lanzaCOnflictException() {
        PlayerStatusUpdateDTO dto = new PlayerStatusUpdateDTO(PlayerState.ACTIVE);
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(existingPlayer));

        assertThatThrownBy(() -> playerService.changeStatus(playerId, dto))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void changeStatus_deRetiredAActive_permiteReactivacion() {
        existingPlayer.setActualState(PlayerState.RETIRED);
        PlayerStatusUpdateDTO dto = new PlayerStatusUpdateDTO(PlayerState.ACTIVE);

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(existingPlayer));
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlayerResponseDTO result = playerService.changeStatus(playerId,dto);

        assertThat(result.actualState()).isEqualTo(PlayerState.ACTIVE);

    }

    // ---------- DELETE ----------

    @Test
    void delete_sinCarerrasCOmpletadas_eliminaCorrectamente() {
        existingPlayer.setRacesCompleted(0);
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(existingPlayer));

        playerService.delete(playerId);

        verify(playerRepository).delete(existingPlayer);
    }

    @Test
    void delete_conCarrerasCompletadas_lanzaConflictExceptionYNoElimina() {
        existingPlayer.setRacesCompleted(3);
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(existingPlayer));

        assertThatThrownBy(() -> playerService.delete(playerId))
                .isInstanceOf(ConflictException.class);

        verify(playerRepository, never()).delete(any(Player.class));
    }
}
