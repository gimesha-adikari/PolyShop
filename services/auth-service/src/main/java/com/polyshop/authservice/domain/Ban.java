package com.polyshop.authservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter @Setter
@Entity
@Table(name = "bans")
public class Ban {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false)
    private String key;
    @Column(nullable=false)
    private Instant until;
    @Column(nullable=true)
    private String reason;
}
