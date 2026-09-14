package com.spring.camelsvsdwarfs.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "standing_results", indexes = {
        @Index(name = "idx_result_race", columnList = "id_race"),
        @Index(name = "idx_result_player", columnList = "id_player"),
        @Index(name = "idx_result_team", columnList = "id_team")
})
// Restricción XOR exigida por la arquitectura polimórfica de participantes
@org.hibernate.annotations.Check(constraints = "(id_player IS NOT NULL AND id_team IS NULL) OR (id_player IS NULL AND id_team IS NOT NULL)")
public class StandingResult {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idStandingResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_race", nullable = false)
    private Race race;

    // Llaves foráneas polimórficas (deben tolerar nulos para el XOR)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_player")
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_team")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    private Integer startPosition;
    private Integer endPosition;

    @Column(length = 100)
    private String completedType;

    @Builder.Default
    @Column(nullable = false)
    private Integer penalizationTimeMs = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultStatus statusResult;

    private Integer totalTimeMs;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}
