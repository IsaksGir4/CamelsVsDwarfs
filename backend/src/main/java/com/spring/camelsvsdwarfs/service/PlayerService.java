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
import com.spring.camelsvsdwarfs.repository.specification.PlayerSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerResponseDTO create(PlayerCreateDTO dto){
        if (playerRepository.existsByNickname(dto.nickname())) {
            throw new ConflictException("Ya existe un competidor con el nickname '" + dto.nickname() + "'");
        }

        Player player = Player.builder()
                .name(dto.name())
                .nickname(dto.nickname())
                .playerType(dto.playerType())
                .birthDate(dto.birthDate())
                .height(dto.height())
                .weight(dto.weight())
                .placeOfBirth(dto.placeOfBirth())
                .actualState(PlayerState.ACTIVE)
                .registerDate(LocalDate.now())
                .victories(0)
                .defeats(0)
                .racesCompleted(0)
                .build();

        Player saved = playerRepository.save(player);
        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public  PlayerResponseDTO findById(UUID id){
        Player player = findEntityById(id);
        return toResponseDTO(player);
    }

    @Transactional(readOnly = true)
    public Page<PlayerResponseDTO> findAll(PlayerType playerType, PlayerState actualState, String name, Pageable pageable) {
        Specification<Player> spec = Specification
                .where(PlayerSpecification.hasType(playerType))
                .and(PlayerSpecification.hasState(actualState))
                .and(PlayerSpecification.nameContains(name));

        return playerRepository.findAll(spec, pageable)
                .map(this::toResponseDTO);
    }

    @Transactional
    public PlayerResponseDTO update(UUID id, PlayerUpdateDTO dto){
        Player player = findEntityById(id);

        //Si se cambia el nickname, se valida que el nuevo no choque con otro jugador
        if (!player.getNickname().equals(dto.nickname())
        && playerRepository.existsByNickname(dto.nickname())){
            throw  new ConflictException("Ya existe un competidor con el nickname '" + dto.nickname() + "'");
        }

        player.setName(dto.name());
        player.setNickname(dto.nickname());
        player.setPlayerType(dto.playerType());
        player.setBirthDate(dto.birthDate());
        player.setHeight(dto.height());
        player.setWeight(dto.weight());
        player.setPlaceOfBirth(dto.placeOfBirth());

        Player updated = playerRepository.save(player);
        return toResponseDTO(updated);
    }

    @Transactional
    public PlayerResponseDTO changeStatus(UUID id, PlayerStatusUpdateDTO dto) {
        Player player = findEntityById(id);
        PlayerState current = player.getActualState();
        PlayerState next = dto.newState();

        if (current == next) {
            throw new ConflictException("El competidor ya se encuentra en estado " + next);
        }
        player.setActualState(next);

        Player updated = playerRepository.save(player);
        return toResponseDTO(updated);
    }

    public void delete(UUID id){
        Player player = findEntityById(id);

        // TODO(feature/results): Esta validación es un PROXY temporal mientras no existe
        // StandingResultRepository (se construye en feature/results). Lo correcto según
        // la rúbrica es verificar existencia real de resultados oficiales asociados a
        // este competidor: standingResultRepository.existsByPlayerId(id).
        // Reemplazar esta condición apenas se integre esa branch a develop, para no
        // dejar borrar físicamente un competidor con carreras completadas pero
        // registradas incorrectamente en racesCompleted (ej. por bug o dato manual).
        if (player.getRacesCompleted() != null && player.getRacesCompleted() > 0){
            throw new ConflictException(
                    "No se puede eliminar un competidor con carreras oficiales registradas. " +
                    "Debe ser retirado (RETIRED) en su lugar."
            );
        }
        playerRepository.delete(player);

    }

    private Player findEntityById(UUID id){
        return playerRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Competidor con ID " + id + " no fue encontrado"));
    }

    private PlayerResponseDTO toResponseDTO(Player player) {
        return new PlayerResponseDTO(
                player.getIdPlayer(),
                player.getName(),
                player.getNickname(),
                player.getPlayerType(),
                player.getBirthDate(),
                player.getHeight(),
                player.getWeight(),
                player.getPlaceOfBirth(),
                player.getActualState(),
                player.getRegisterDate(),
                player.getVictories(),
                player.getDefeats(),
                player.getRacesCompleted()
        );
    }
}
