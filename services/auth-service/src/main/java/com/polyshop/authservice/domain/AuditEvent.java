package com.polyshop.authservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter @Setter
@Entity
@Table(name = "audit_events", indexes = {@Index(columnList = "eventType")})
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String eventType;
    private Long userId;
    @Column(length = 2000)
    private String message;
    private Instant createdAt;
    @PrePersist
    public void pre() { createdAt = Instant.now(); }
}
