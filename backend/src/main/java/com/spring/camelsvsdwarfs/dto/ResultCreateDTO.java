package com.spring.camelsvsdwarfs.dto;

import com.spring.camelsvsdwarfs.entity.ResultStatus;
import jakarta.validation.constraints.*;

import java.util.UUID;

public record ResultCreateDTO(
        UUID playerId,

        UUID teamId,

        @NotNull(message = "El estado del resultado es obligatorio")
        ResultStatus statusResult,

        @Positive(message = "El tiempo total debe ser positivo")
        Integer totalTimeMs,

        Integer endPosition,

        @PositiveOrZero(message = "El tiempo de penalizacion no puede ser negativo")
        Integer penalizationTimeMs,

        @Size(max = 100)
        String completedType,

        @Size(max = 512)
        String notes
) {
    @AssertTrue(message = "Debe indicarse exactamente un participante: jugador o equipo, no ambos ni ninguno")
    public boolean isExactlyOneParticipant() {
        return (playerId != null) ^ (teamId != null);
    }
}