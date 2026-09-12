package com.spring.camelsvsdwarfs.dto;

import com.spring.camelsvsdwarfs.entity.TeamCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TeamCreateDTO(
        @NotBlank(message = "El nombre del equipo no puede estar vacio")
        @Size(min = 3, max = 100, message = "Entre 3 y 100 caracteres")
        String teamName,

        @Size(max = 500, message = "La descripcion no puede superar los 500 caracteres")
        String description,

        @Size(max = 100, message = "El nombre del coach no puede superar los 100 caracteres")
        String responsableCoach,

        @NotNull(message = "El maximo de miembros es obligatorio")
        TeamCategory category
) {
}
