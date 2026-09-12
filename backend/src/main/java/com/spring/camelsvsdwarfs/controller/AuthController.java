package com.spring.camelsvsdwarfs.controller;

import com.spring.camelsvsdwarfs.entity.User;
import com.spring.camelsvsdwarfs.service.UserSyncService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserSyncService userSyncService;

    public AuthController(UserSyncService userSyncService) {
        this.userSyncService = userSyncService;
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(@AuthenticationPrincipal Jwt jwt) {
        User user = userSyncService.findOrCreateUser(jwt);
        return Map.of(
                "id", user.getIdUser(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "roles", jwt.getClaimAsMap("realm_access").get("roles")
        );
    }
}