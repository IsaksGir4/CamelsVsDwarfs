package com.spring.camelsvsdwarfs.dto;

import com.spring.camelsvsdwarfs.entity.TeamStatus;
import jakarta.validation.constraints.NotNull;

public record TeamStatusUpdateDTO(
        @NotNull(message = "El nuevo estado es obligatorio")
        TeamStatus newState
) {
}
