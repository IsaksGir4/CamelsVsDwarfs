package com.spring.camelsvsdwarfs.controller;

import tools.jackson.databind.json.JsonMapper;
import com.spring.camelsvsdwarfs.config.SecurityConfig;
import com.spring.camelsvsdwarfs.dto.RaceCreateDTO;
import com.spring.camelsvsdwarfs.dto.RaceResponseDTO;
import com.spring.camelsvsdwarfs.entity.RaceStatus;
import com.spring.camelsvsdwarfs.entity.RaceType;
import com.spring.camelsvsdwarfs.exception.ResourceNotFoundException;
import com.spring.camelsvsdwarfs.service.RaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de seguridad para RaceController — verifica que @PreAuthorize
 * se cumple correctamente a nivel HTTP, no solo que el código compila.
 */
@WebMvcTest(RaceController.class)
@Import(SecurityConfig.class)
class RaceControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private RaceService raceService;

    private RaceCreateDTO validRaceDto() {
        return new RaceCreateDTO(
                "Carrera de prueba",
                "Descripcion",
                LocalDate.now().plusDays(10),
                LocalTime.of(9, 0),
                "Alto de las Palmas",
                "Envigado",
                1000,
                4,
                RaceType.INDIVIDUAL,
                LocalDate.now().plusDays(5)
        );
    }

    @Test
    void crearCarrera_comoViewer_retorna403() throws Exception {
        mockMvc.perform(post("/api/races")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRaceDto())))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearCarrera_comoAdmin_retorna201() throws Exception {
        RaceResponseDTO response = new RaceResponseDTO(
                UUID.randomUUID(), UUID.randomUUID(), "admin",
                "Carrera de prueba", "Descripcion",
                LocalDate.now().plusDays(10), LocalTime.of(9, 0),
                "Alto de las Palmas", "Envigado",
                1000, 4, RaceType.INDIVIDUAL, RaceStatus.DRAFT,
                LocalDate.now().plusDays(5), null, null
        );
        when(raceService.create(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/races")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRaceDto())))
                .andExpect(status().isCreated());
    }

    @Test
    void crearCarrera_sinToken_retorna401() throws Exception {
        mockMvc.perform(post("/api/races")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRaceDto())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void buscarCarreraInexistente_comoAdmin_retorna404() throws Exception {
        UUID randomId = UUID.randomUUID();
        when(raceService.findById(randomId))
                .thenThrow(new ResourceNotFoundException("Race con ID " + randomId + " no fue encontrada"));

        mockMvc.perform(get("/api/races/" + randomId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }
}