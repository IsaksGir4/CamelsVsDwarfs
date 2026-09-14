package com.spring.camelsvsdwarfs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRejectDTO(
        @NotBlank(message = "El motivo del rechazo es obligatorio")
        @Size(max = 512, message = "El motivo no puede superar los 512 caracteres")
        String reason
) {
}