package com.spring.camelsvsdwarfs.dto;

import com.spring.camelsvsdwarfs.entity.RaceStatus;
import com.spring.camelsvsdwarfs.entity.RaceType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record RaceResponseDTO(
        UUID idRace,
        UUID organizerId,
        String organizerUsername,
        String raceName,
        String description,
        LocalDate programationDate,
        LocalTime programationHour,
        String ubicationStart,
        String ubicationFinish,
        Integer distanceMeters,
        Integer maxPlayers,
        RaceType raceType,
        RaceStatus raceStatus,
        LocalDate registrationDeadline,
        LocalDateTime creationDate,
        LocalDateTime lastUpdate
) {
}