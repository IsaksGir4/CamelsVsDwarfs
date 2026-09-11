package com.spring.camelsvsdwarfs.service;

import com.spring.camelsvsdwarfs.entity.User;
import com.spring.camelsvsdwarfs.repository.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSyncService {

    private final UserRepository userRepository;

    public UserSyncService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Finds the local User row matching this Keycloak identity, creating it
     * on first sight. Called on every authenticated request that needs a
     * local User to attach to (registrations, results, audit logs, etc.) —
     * so no manual user provisioning is ever needed.
     */
    @Transactional
    public User findOrCreateUser(Jwt jwt) {
        String keycloakId = jwt.getSubject(); // the "sub" claim
        return userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .keycloakId(keycloakId)
                                .username(jwt.getClaimAsString("preferred_username"))
                                .email(jwt.getClaimAsString("email"))
                                .build()
                ));
    }
}