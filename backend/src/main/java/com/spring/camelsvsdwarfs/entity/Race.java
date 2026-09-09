package com.spring.camelsvsdwarfs.entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="races")
public class Race {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idRace;

    @Column(name = "id_organizer", nullable = false, length = 100)
    private String organizerUsername;

    @Column(nullable = false, length = 100)
    private String raceName;

    @Column(nullable = false, length = 512)
    private String description;

    @Column(nullable = false)
    private LocalDate programationDate;

    @Column(nullable = false)
    private LocalTime programationHour;

    @Column(nullable = false, length = 100)
    private String ubicationStart;

    @Column(nullable = false, length = 100)
    private String ubicationFinish;

    @Column(nullable = false)
    private Integer distanceMeters;

    @Column(nullable = false)
    private Integer maxPlayers;

    @Enumerated(EnumType.STRING)
    @Column(name = "race_type", nullable = false)
    private RaceType raceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "race_status", nullable = false)
    private RaceStatus raceStatus;

    @Column(nullable = false)
    private LocalDate registrationDeadline;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime creatiojnDate = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime lastUpdate = LocalDateTime.now();
}
