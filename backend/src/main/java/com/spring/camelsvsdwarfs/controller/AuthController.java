package com.spring.camelsvsdwarfs.controller;

import com.spring.camelsvsdwarfs.dto.UserProfileDTO;
import com.spring.camelsvsdwarfs.entity.User;
import com.spring.camelsvsdwarfs.service.UserSyncService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserSyncService userSyncService;

    public AuthController(UserSyncService userSyncService) {
        this.userSyncService = userSyncService;
    }

    @GetMapping("/profile")
    public UserProfileDTO profile(@AuthenticationPrincipal Jwt jwt) {
        User user = userSyncService.findOrCreateUser(jwt);

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        @SuppressWarnings("unchecked")
        List<String> roles = realmAccess != null
                ? (List<String>) realmAccess.getOrDefault("roles", Collections.emptyList())
                : Collections.emptyList();

        return new UserProfileDTO(user.getIdUser(), user.getUsername(), user.getEmail(), roles);
    }
}