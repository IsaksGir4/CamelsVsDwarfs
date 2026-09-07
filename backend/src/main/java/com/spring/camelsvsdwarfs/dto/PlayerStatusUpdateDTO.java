package com.spring.camelsvsdwarfs.dto;

import com.spring.camelsvsdwarfs.entity.PlayerState;
import jakarta.validation.constraints.NotNull;

public record PlayerStatusUpdateDTO(
        @NotNull(message = "El nuevo estado es obligatorio")
        PlayerState newState
) {
}
