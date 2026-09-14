package com.spring.camelsvsdwarfs.dto;

import java.util.List;
import java.util.UUID;

public record UserProfileDTO(
        UUID id,
        String username,
        String email,
        List<String> roles
) {
}