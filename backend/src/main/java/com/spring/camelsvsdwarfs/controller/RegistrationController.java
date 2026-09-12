package com.spring.camelsvsdwarfs.controller;

import com.spring.camelsvsdwarfs.dto.RegistrationCreateDTO;
import com.spring.camelsvsdwarfs.dto.RegistrationRejectDTO;
import com.spring.camelsvsdwarfs.dto.RegistrationResponseDTO;
import com.spring.camelsvsdwarfs.entity.RegistrationStatus;
import com.spring.camelsvsdwarfs.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/races/{raceId}/registrations")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<RegistrationResponseDTO> create(@PathVariable UUID raceId,
                                                          @Valid @RequestBody RegistrationCreateDTO dto,
                                                          @AuthenticationPrincipal Jwt jwt) {
        RegistrationResponseDTO created = registrationService.create(raceId, dto, jwt);
        return ResponseEntity
                .created(URI.create("/api/registrations/" + created.idRegister()))
                .body(created);
    }

    @GetMapping("/races/{raceId}/registrations")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'VIEWER')")
    public ResponseEntity<List<RegistrationResponseDTO>> findByRace(
            @PathVariable UUID raceId,
            @RequestParam(required = false) RegistrationStatus status) {
        return ResponseEntity.ok(registrationService.findByRace(raceId, status));
    }

    @GetMapping("/registrations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'VIEWER')")
    public ResponseEntity<RegistrationResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(registrationService.findById(id));
    }

    @PatchMapping("/registrations/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<RegistrationResponseDTO> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(registrationService.approve(id));
    }

    @PatchMapping("/registrations/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<RegistrationResponseDTO> reject(@PathVariable UUID id,
                                                          @Valid @RequestBody RegistrationRejectDTO dto) {
        return ResponseEntity.ok(registrationService.reject(id, dto));
    }

    @DeleteMapping("/registrations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id) {
        registrationService.cancel(id);
    }
}