package com.spring.camelsvsdwarfs.dto;

import com.spring.camelsvsdwarfs.entity.TeamCategory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TeamCreateDTOValidationTest {

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
        TeamCreateDTO dto = new TeamCreateDTO(
                "The Five Exceptions", "Equipo de enanos", "Mr. Abandonado", TeamCategory.QUARTET
        );

        Set<ConstraintViolation<TeamCreateDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void nombreVacio_esRechazadoPorValidacion() {
        TeamCreateDTO dto = new TeamCreateDTO(
                "", "Equipo de enanos", "Mr. Abandonado", TeamCategory.QUARTET
        );

        Set<ConstraintViolation<TeamCreateDTO>> violations = validator.validate(dto);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("teamName"));
    }

    @Test
    void nombreDemasiadoCorto_esRechazadoPorValidacion() {
        TeamCreateDTO dto = new TeamCreateDTO(
                "AB", "Equipo de enanos", "Mr. Abandonado", TeamCategory.QUARTET
        );

        Set<ConstraintViolation<TeamCreateDTO>> violations = validator.validate(dto);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("teamName"));
    }

    @Test
    void categoriaNula_esRechazadaPorValidacion() {
        TeamCreateDTO dto = new TeamCreateDTO(
                "The Five Exceptions", "Equipo de enanos", "Mr. Abandonado", null
        );

        Set<ConstraintViolation<TeamCreateDTO>> violations = validator.validate(dto);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("category"));
    }

    @Test
    void descripcionDemasiadoLarga_esRechazadaPorValidacion() {
        String descripcionLarga = "A".repeat(501);
        TeamCreateDTO dto = new TeamCreateDTO(
                "The Five Exceptions", descripcionLarga, "Mr. Abandonado", TeamCategory.QUARTET
        );

        Set<ConstraintViolation<TeamCreateDTO>> violations = validator.validate(dto);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }
}