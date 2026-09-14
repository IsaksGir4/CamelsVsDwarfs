package com.spring.camelsvsdwarfs.service;

import com.spring.camelsvsdwarfs.entity.AuditLog;
import com.spring.camelsvsdwarfs.entity.User;
import com.spring.camelsvsdwarfs.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Registro de auditoria (modulo 8).
 *
 * Se expone en dos formas:
 *  - log(jwt, ...)  cuando quien llama ya tiene el Jwt a la mano.
 *  - log(action, ...) que resuelve el usuario desde el SecurityContext, para
 *    no tener que propagar el Jwt por toda la cadena de servicios.
 *
 * Corre en una transaccion propia (REQUIRES_NEW) y captura sus errores: un
 * fallo al auditar nunca debe tumbar la operacion de negocio.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserSyncService userSyncService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Jwt jwt, String action, String entityType, UUID entityId,
                    String description, String oldValue, String newValue) {
        try {
            User user = jwt != null ? userSyncService.findOrCreateUser(jwt) : null;

            auditLogRepository.save(AuditLog.builder()
                    .user(user)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .build());
        } catch (Exception e) {
            log.warn("No se pudo registrar la auditoria de '{}' sobre {} {}: {}",
                    action, entityType, entityId, e.getMessage());
        }
    }

    /** Variante que toma el usuario autenticado del contexto de seguridad. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, UUID entityId,
                    String description, String oldValue, String newValue) {
        log(currentJwt(), action, entityType, entityId, description, oldValue, newValue);
    }

    /** Atajo para acciones sin valores previos ni nuevos. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, UUID entityId, String description) {
        log(currentJwt(), action, entityType, entityId, description, null, null);
    }

    private Jwt currentJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof Jwt jwt ? jwt : null;
    }
}