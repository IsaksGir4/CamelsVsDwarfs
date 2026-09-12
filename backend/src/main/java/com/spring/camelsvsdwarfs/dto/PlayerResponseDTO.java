package com.spring.camelsvsdwarfs.dto;

import com.spring.camelsvsdwarfs.entity.PlayerState;
import com.spring.camelsvsdwarfs.entity.PlayerType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PlayerResponseDTO(
        UUID idPlayer,
        String name,
        String nickname,
        PlayerType playerType,
        LocalDate birthDate,
        BigDecimal height,
        BigDecimal weight,
        String placeOfBirth,
        PlayerState actualState,
        LocalDate registerDate,
        Integer victories,
        Integer defeats,
        Integer racesCompleted
//        String currentTeamName
) {
}
