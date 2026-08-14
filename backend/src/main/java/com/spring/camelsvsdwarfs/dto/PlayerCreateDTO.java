package com.spring.camelsvsdwarfs.dto;

import com.spring.camelsvsdwarfs.entity.PlayerState;
import com.spring.camelsvsdwarfs.entity.PlayerType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PlayerCreateDTO(

        @NotBlank(message = "El nombre no puede estar vacio")
        @Size(min=5, max = 100, message = "Entre 5 y 100 caracteres")
        String name,
        @NotBlank(message = "El nickname no puede estar vacio")
        @Size(min=1, max=50, message = "Entre 1 y 50 caracteres")
        String nickname,
        @NotNull(message = "El tipo de competidor es obligatorio")
        PlayerType playerType,
        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha debe ser en el pasado")
        LocalDate birthDate,
        @NotNull(message = "La altura es obligatoria")
        @Positive(message = "La altura debe ser mayor a 0")
        @DecimalMax(value="5.00",message = "La altura fuera de rango razonable")
        BigDecimal height,
        @NotNull(message = "El peso es obligatorio")
        @Positive(message = "El peso debe ser mayor a 0")
        @DecimalMax(value="1000.00", message = "Peso fuera de rango razonable")
        BigDecimal weight,
        @NotBlank(message = "El lugar de nacimiento no puede estar vacio")
        @Size(max = 100)
        String placeOfBirth
        ){

}
