package com.spring.camelsvsdwarfs.dto;

import com.spring.camelsvsdwarfs.entity.ResultStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResultResponseDTO(
        UUID idStandingResult,
        UUID raceId,
        UUID playerId,
        String playerNickname,
        UUID teamId,
        String teamName,
        UUID registeredByUserId,
        Integer startPosition,
        Integer endPosition,
        String completedType,
        Integer penalizationTimeMs,
        ResultStatus statusResult,
        Integer totalTimeMs,
        String notes,
        LocalDateTime timestamp
) {
}