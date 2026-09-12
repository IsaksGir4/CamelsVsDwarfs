package com.spring.camelsvsdwarfs.controller;

import com.spring.camelsvsdwarfs.dto.TeamCreateDTO;
import com.spring.camelsvsdwarfs.dto.TeamMemberResponseDTO;
import com.spring.camelsvsdwarfs.dto.TeamResponseDTO;
import com.spring.camelsvsdwarfs.dto.TeamStatusUpdateDTO;
import com.spring.camelsvsdwarfs.dto.TeamUpdateDTO;
import com.spring.camelsvsdwarfs.entity.TeamStatus;
import com.spring.camelsvsdwarfs.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamResponseDTO> create(@Valid @RequestBody TeamCreateDTO dto) {
        TeamResponseDTO created = teamService.create(dto);
        return ResponseEntity
                .created(URI.create("/api/teams/" + created.idTeam()))
                .body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'VIEWER')")
    public ResponseEntity<Page<TeamResponseDTO>> findAll(
            @RequestParam(required = false) TeamStatus status,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 10, sort = "teamName") Pageable pageable) {
        Page<TeamResponseDTO> result = teamService.findAll(status, name, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'VIEWER')")
    public ResponseEntity<TeamResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(teamService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody TeamUpdateDTO dto) {
        return ResponseEntity.ok(teamService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamResponseDTO> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody TeamStatusUpdateDTO dto) {
        return ResponseEntity.ok(teamService.changeStatus(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        teamService.delete(id);
    }

    @GetMapping("/{teamId}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'VIEWER')")
    public ResponseEntity<List<TeamMemberResponseDTO>> getActiveMembers(@PathVariable UUID teamId) {
        return ResponseEntity.ok(teamService.getActiveMembers(teamId));
    }

    @PostMapping("/{teamId}/members/{competitorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamMemberResponseDTO> addMember(
            @PathVariable UUID teamId,
            @PathVariable UUID competitorId) {
        TeamMemberResponseDTO added = teamService.addMember(teamId, competitorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(added);
    }

    @DeleteMapping("/{teamId}/members/{competitorId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable UUID teamId,
            @PathVariable UUID competitorId) {
        teamService.removeMember(teamId, competitorId);
    }
}