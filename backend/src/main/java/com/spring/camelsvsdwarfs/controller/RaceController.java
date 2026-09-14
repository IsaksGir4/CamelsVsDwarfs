package com.spring.camelsvsdwarfs.controller;

import com.spring.camelsvsdwarfs.dto.RaceCreateDTO;
import com.spring.camelsvsdwarfs.dto.RaceResponseDTO;
import com.spring.camelsvsdwarfs.dto.RaceStatusUpdateDTO;
import com.spring.camelsvsdwarfs.dto.RaceUpdateDTO;
import com.spring.camelsvsdwarfs.entity.RaceStatus;
import com.spring.camelsvsdwarfs.entity.RaceType;
import com.spring.camelsvsdwarfs.service.RaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/races")
@RequiredArgsConstructor
public class RaceController {

    private final RaceService raceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<RaceResponseDTO> create(@Valid @RequestBody RaceCreateDTO dto,
                                                  @AuthenticationPrincipal Jwt jwt) {
        RaceResponseDTO created = raceService.create(dto, jwt);
        return ResponseEntity
                .created(URI.create("/api/races/" + created.idRace()))
                .body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'VIEWER')")
    public ResponseEntity<Page<RaceResponseDTO>> findAll(
            @RequestParam(required = false) RaceStatus status,
            @RequestParam(required = false) RaceType type,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledAfter,
            @PageableDefault(size = 10, sort = "programationDate") Pageable pageable) {
        return ResponseEntity.ok(raceService.findAll(status, type, name, scheduledAfter, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'VIEWER')")
    public ResponseEntity<RaceResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(raceService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<RaceResponseDTO> update(@PathVariable UUID id,
                                                  @Valid @RequestBody RaceUpdateDTO dto) {
        return ResponseEntity.ok(raceService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<RaceResponseDTO> changeStatus(@PathVariable UUID id,
                                                        @Valid @RequestBody RaceStatusUpdateDTO dto) {
        return ResponseEntity.ok(raceService.changeStatus(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        raceService.delete(id);
    }
}