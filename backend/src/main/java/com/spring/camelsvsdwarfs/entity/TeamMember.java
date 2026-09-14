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
@Table(name = "team_members", indexes = {
        @Index(name = "idx_member_team_status", columnList = "id_team, status"),
        @Index(name = "idx_member_player_status", columnList = "id_player, status")
})
public class TeamMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idTeamMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="id_team", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_player", nullable = false)
    private Player player;

    @Column(nullable = false)
    private LocalDate joinDate;

    @Column
    private LocalDate leaveDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamMemberStatus status = TeamMemberStatus.ACTIVE;

}
