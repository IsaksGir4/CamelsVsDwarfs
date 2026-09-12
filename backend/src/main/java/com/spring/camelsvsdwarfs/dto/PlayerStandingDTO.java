package com.spring.camelsvsdwarfs.dto;

import java.util.UUID;

public record PlayerStandingDTO(
        UUID playerId,
        String name,
        String nickname,
        Integer points,
        Integer victories,
        Integer defeats,
        Integer racesCompleted
) {
}