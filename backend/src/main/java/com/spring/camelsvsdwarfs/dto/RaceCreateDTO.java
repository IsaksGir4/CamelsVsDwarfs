package com.spring.camelsvsdwarfs.dto;

import com.spring.camelsvsdwarfs.entity.RaceType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

public record RaceCreateDTO(
        @NotBlank(message = "El nombre de la carrera no puede estar vacio")
        @Size(min = 3, max = 100, message = "Entre 3 y 100 caracteres")
        String raceName,

        @Size(max = 512, message = "La descripcion no puede superar los 512 caracteres")
        String description,

        @NotNull(message = "La fecha programada es obligatoria")
        @FutureOrPresent(message = "La carrera no puede programarse en el pasado")
        LocalDate programationDate,

        @NotNull(message = "La hora programada es obligatoria")
        LocalTime programationHour,

        @NotBlank(message = "La ubicacion de inicio es obligatoria")
        @Size(max = 100)
        String ubicationStart,

        @NotBlank(message = "La ubicacion de finalizacion es obligatoria")
        @Size(max = 100)
        String ubicationFinish,

        @NotNull(message = "La distancia es obligatoria")
        @Positive(message = "La distancia debe ser mayor a cero")
        Integer distanceMeters,

        @NotNull(message = "El maximo de participantes es obligatorio")
        @Min(value = 2, message = "Se requieren al menos 2 participantes")
        Integer maxPlayers,

        @NotNull(message = "El tipo de carrera es obligatorio")
        RaceType raceType,

        @NotNull(message = "La fecha limite de registro es obligatoria")
        LocalDate registrationDeadline
) {
}