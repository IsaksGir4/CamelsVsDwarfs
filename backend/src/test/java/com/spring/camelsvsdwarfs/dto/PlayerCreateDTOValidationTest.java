package com.spring.camelsvsdwarfs.dto;

import com.spring.camelsvsdwarfs.entity.PlayerType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerCreateDTOValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    @Test
    void dtoValido_noProduceViolaciones() {
        PlayerCreateDTO dto = new PlayerCreateDTO(
                "ByteAmel", "ByteTheCamel", PlayerType.CAMEL,
                LocalDate.of(2018, 5, 10),
                new BigDecimal("2.10"), new BigDecimal("450.00"),
                "Alto de las Palmas"
        );

        Set<ConstraintViolation<PlayerCreateDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void pesoNegativo_esRechazadoPorValidacion() {
        PlayerCreateDTO dto = new PlayerCreateDTO(
                "ByteAmel", "ByteTheCamel", PlayerType.CAMEL,
                LocalDate.of(2018, 5, 10),
                new BigDecimal("2.10"), new BigDecimal("-10.00"),
                "Alto de las Palmas"
        );

        Set<ConstraintViolation<PlayerCreateDTO>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("weight"));
    }

    @Test
    void nicknameVacio_esRechazadoPorValidacion() {
        PlayerCreateDTO dto = new PlayerCreateDTO(
                "ByteAmel", "", PlayerType.CAMEL,
                LocalDate.of(2018, 5, 10),
                new BigDecimal("2.10"), new BigDecimal("450.00"),
                "Alto de las Palmas"
        );

        Set<ConstraintViolation<PlayerCreateDTO>> violations = validator.validate(dto);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("nickname"));
    }

    @Test
    void fechaNacimientoEnElFuturo_esRechazadaPorValidacion() {
        PlayerCreateDTO dto = new PlayerCreateDTO(
                "ByteAmel", "ByteTheCamel", PlayerType.CAMEL,
                LocalDate.now().plusDays(1),
                new BigDecimal("2.10"), new BigDecimal("450.00"),
                "Alto de las Palmas"
        );

        Set<ConstraintViolation<PlayerCreateDTO>> violations = validator.validate(dto);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("birthDate"));
    }
}