package com.spring.camelsvsdwarfs.dto;

import java.util.UUID;

public record TeamStandingDTO(
        UUID teamId,
        String teamName,
        Integer points,
        Integer victories,
        Integer defeats
) {
}