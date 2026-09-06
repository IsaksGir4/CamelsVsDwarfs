package com.spring.camelsvsdwarfs.controller;

import com.spring.camelsvsdwarfs.dto.PlayerCreateDTO;
import com.spring.camelsvsdwarfs.dto.PlayerResponseDTO;
import com.spring.camelsvsdwarfs.dto.PlayerStatusUpdateDTO;
import com.spring.camelsvsdwarfs.dto.PlayerUpdateDTO;
import com.spring.camelsvsdwarfs.entity.PlayerState;
import com.spring.camelsvsdwarfs.entity.PlayerType;
import com.spring.camelsvsdwarfs.service.PlayerService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {
    private final PlayerService playerService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlayerResponseDTO> create(@Valid @RequestBody PlayerCreateDTO dto) {
        PlayerResponseDTO created = playerService.create(dto);
        return ResponseEntity
                .created(URI.create("/api/players/" + created.idPlayer()))
                .body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'VIEWER')")
    public ResponseEntity<Page<PlayerResponseDTO>> findAll(
            @RequestParam(required = false) PlayerType playerType,
            @RequestParam(required = false) PlayerState actualState,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
                Page<PlayerResponseDTO> result = playerService.findAll(playerType, actualState, name, pageable);
                return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER', 'VIEWER')")
    public ResponseEntity<PlayerResponseDTO> findById(@PathVariable UUID id){
        return ResponseEntity.ok(playerService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlayerResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody PlayerUpdateDTO dto){
        return ResponseEntity.ok(playerService.update(id,dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlayerResponseDTO> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody PlayerStatusUpdateDTO dto
            ){
        return ResponseEntity.ok(playerService.changeStatus(id,dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete (@PathVariable UUID id){
        playerService.delete(id);
    }


}
