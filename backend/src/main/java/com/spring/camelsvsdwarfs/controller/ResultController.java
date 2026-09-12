package com.spring.camelsvsdwarfs.controller;

import com.spring.camelsvsdwarfs.dto.PlayerStandingDTO;
import com.spring.camelsvsdwarfs.dto.ResultCreateDTO;
import com.spring.camelsvsdwarfs.dto.ResultResponseDTO;
import com.spring.camelsvsdwarfs.dto.ResultUpdateDTO;
import com.spring.camelsvsdwarfs.dto.TeamStandingDTO;
import com.spring.camelsvsdwarfs.service.ResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;

    @PostMapping("/api/races/{raceId}/results")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<ResultResponseDTO> create(
            @PathVariable UUID raceId,
            @Valid @RequestBody ResultCreateDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        ResultResponseDTO created = resultService.create(raceId, dto, jwt);
        return ResponseEntity
                .created(URI.create("/api/results/" + created.idStandingResult()))
                .body(created);
    }

    @GetMapping("/api/races/{raceId}/results")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'VIEWER')")
    public ResponseEntity<List<ResultResponseDTO>> findByRace(@PathVariable UUID raceId) {
        return ResponseEntity.ok(resultService.findByRace(raceId));
    }

    @GetMapping("/api/results/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'VIEWER')")
    public ResponseEntity<ResultResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(resultService.findById(id));
    }

    @PutMapping("/api/results/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<ResultResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody ResultUpdateDTO dto) {
        return ResponseEntity.ok(resultService.update(id, dto));
    }

    @GetMapping("/api/standings")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getStandings() {
        return ResponseEntity.ok(Map.of(
                "competitors", resultService.getPlayerStandings(),
                "teams", resultService.getTeamStandings()
        ));
    }

    @GetMapping("/api/standings/competitors")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'VIEWER')")
    public ResponseEntity<List<PlayerStandingDTO>> getCompetitorStandings() {
        return ResponseEntity.ok(resultService.getPlayerStandings());
    }

    @GetMapping("/api/standings/teams")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'VIEWER')")
    public ResponseEntity<List<TeamStandingDTO>> getTeamStandings() {
        return ResponseEntity.ok(resultService.getTeamStandings());
    }
}