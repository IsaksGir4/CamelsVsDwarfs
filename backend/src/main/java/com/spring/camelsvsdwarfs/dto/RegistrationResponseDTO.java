package com.spring.camelsvsdwarfs.dto;

import com.spring.camelsvsdwarfs.entity.RegistrationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegistrationResponseDTO(
        UUID idRegister,
        UUID raceId,
        String raceName,
        String participantType,   // "PLAYER" o "TEAM"
        UUID playerId,
        String playerNickname,
        UUID teamId,
        String teamName,
        String registeredBy,
        LocalDateTime registerDate,
        RegistrationStatus status,
        Integer assignedLane,
        Integer startPosition,
        String validationNotes
) {
}