package com.spring.camelsvsdwarfs.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Se inscribe un competidor O un equipo, nunca ambos (misma regla XOR
 * que el @Check de la tabla register_player).
 */
public record RegistrationCreateDTO(
        UUID playerId,
        UUID teamId,

        @Positive(message = "La posicion de salida debe ser positiva")
        Integer startPosition,

        @Positive(message = "El carril asignado debe ser positivo")
        Integer assignedLane
) {
    @AssertTrue(message = "Debe indicar exactamente uno: playerId o teamId")
    public boolean isExactlyOneParticipant() {
        return (playerId == null) != (teamId == null);
    }
}