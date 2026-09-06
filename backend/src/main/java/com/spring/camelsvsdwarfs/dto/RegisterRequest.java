package com.spring.camelsvsdwarfs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String username;

    @NotBlank
    @Email(message = "Debe ser un email valido")
    private String email;

    @NotBlank
    private String password;
}