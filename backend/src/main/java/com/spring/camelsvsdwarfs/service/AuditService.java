package com.spring.camelsvsdwarfs.service;

import com.spring.camelsvsdwarfs.entity.AuditLog;
import com.spring.camelsvsdwarfs.entity.User;
import com.spring.camelsvsdwarfs.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserSyncService userSyncService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Jwt jwt, String action, String entityType, UUID entityId,
                    String description, String oldValue, String newValue) {

        User user = jwt != null ? userSyncService.findOrCreateUser(jwt) : null;

        AuditLog entry = AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();

        auditLogRepository.save(entry);
    }
}