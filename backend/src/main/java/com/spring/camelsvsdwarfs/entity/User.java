package com.spring.camelsvsdwarfs.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idUser;

    // Matches Keycloak's "sub" claim — the stable, unique ID Keycloak assigns
    // per account. This is how we link a local row back to its Keycloak identity.
    @Column(nullable = false, unique = true, length = 100)
    private String keycloakId;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(unique=true,length = 150)
    private String email;
}