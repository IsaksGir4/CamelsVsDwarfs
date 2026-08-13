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
@Table(name="register_player")
//Restriccion a nivel de base de datos para la logica XOR
@org.hibernate.annotations.Check(constraints = "(id_player IS NOT NULL AND id_team IS NULL) OR (id_player IS NULL AND id_team IS NOT NULL)")
public class RegisterPlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idRegister;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="id_race", nullable = false)
    private Race race;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_player")
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="id_team")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="id_user", nullable = false)
    private User registeringUser;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime registerDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status;

    private Integer assignedLane;
    private Integer startPosition;

    @Column(length = 512)
    private String validationNotes;
}
