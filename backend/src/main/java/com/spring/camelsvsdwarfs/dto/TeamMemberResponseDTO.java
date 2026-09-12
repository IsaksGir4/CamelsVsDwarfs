package com.spring.camelsvsdwarfs.dto;

import com.spring.camelsvsdwarfs.entity.TeamMemberStatus;

import java.time.LocalDate;
import java.util.UUID;

public record TeamMemberResponseDTO(
        UUID idTeamMember,
        UUID playerId,
        String playerName,
        String playerNickname,
        LocalDate joinDate,
        LocalDate leaveDate,
        TeamMemberStatus status
) {
}
