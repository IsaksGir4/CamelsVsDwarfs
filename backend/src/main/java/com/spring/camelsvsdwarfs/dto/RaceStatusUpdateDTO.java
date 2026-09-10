package com.spring.camelsvsdwarfs.dto;

import com.spring.camelsvsdwarfs.entity.RaceStatus;
import jakarta.validation.constraints.NotNull;

public record RaceStatusUpdateDTO(
        @NotNull(message = "El nuevo estado es obligatorio")
        RaceStatus newStatus
) {
}