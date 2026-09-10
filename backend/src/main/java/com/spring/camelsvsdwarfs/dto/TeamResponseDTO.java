package com.spring.camelsvsdwarfs.dto;

import com.spring.camelsvsdwarfs.entity.TeamCategory;
import com.spring.camelsvsdwarfs.entity.TeamStatus;

import java.time.LocalDate;
import java.util.UUID;

public record TeamResponseDTO(
        UUID idTeam,
        String teamName,
        String description,
        LocalDate creationDate,
        TeamStatus status,
        String responsableCoach,
        TeamCategory category,
        Integer currentMemberCount,
        Integer victories,
        Integer defeats
) {
}
