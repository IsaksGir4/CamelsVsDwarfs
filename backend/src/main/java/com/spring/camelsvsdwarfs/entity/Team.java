package com.spring.camelsvsdwarfs.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="teams")
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idTeam;

    @Column(nullable = false, unique = true, length = 100)
    private String teamName;

    @Column(nullable = false, length = 512)
    private String description;

    @Column(nullable = false)
    private LocalDate creationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamStatus status;

    private String responsibleCoach;

    @Builder.Default
    @Column(nullable = false)
    private Integer victories = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer defeats = 0;
}
