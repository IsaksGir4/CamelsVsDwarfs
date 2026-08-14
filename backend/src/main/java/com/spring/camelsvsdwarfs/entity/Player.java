package com.spring.camelsvsdwarfs.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="players")
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idPlayer;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name="player_type", nullable = false)
    private PlayerType playerType;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(precision = 3, scale = 2)
    private BigDecimal height;

    @Column(precision = 5, scale =2)
    private BigDecimal weight;

    @Column(nullable = false)
    private String placeOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "actual_state", nullable = false)
    private PlayerState actualState;

    @Column(nullable = false)
    private LocalDate registerDate;

    @Builder.Default
    @Column(nullable = false)
    private Integer victories  = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer defeats = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer racesCompleted = 0;
}
