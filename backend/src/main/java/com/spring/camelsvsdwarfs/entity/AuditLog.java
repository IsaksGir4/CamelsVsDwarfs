package com.spring.camelsvsdwarfs.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name="audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idAuditLog;

    @Column(name = "id_user")
    private UUID idUser;

    @Column(nullable = false)
    private String action;

    private String entityType;

    @Column(nullable = false)
    private String entityId;

    @Column(length = 512)
    private String description;

    @Column(length = 512)
    private String oldValue;

    @Column(length = 512)
    private String newValue;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}
