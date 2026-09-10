package com.spring.camelsvsdwarfs.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TeamMemberAddDTO(
        @NotNull(message = "El id del competidor es obligatorio")
        UUID playerId
) {
}
