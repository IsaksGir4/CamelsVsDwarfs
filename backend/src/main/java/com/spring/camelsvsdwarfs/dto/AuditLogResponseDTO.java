package com.spring.camelsvsdwarfs.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponseDTO(
        UUID idAuditLog,
        String username,
        String action,
        String entityType,
        UUID entityId,
        String description,
        String oldValue,
        String newValue,
        LocalDateTime timestamp
) {
}