package com.helisa.docmanager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "revoked_tokens", indexes = {
        @Index(name = "idx_revoked_jti", columnList = "jti", unique = true),
        @Index(name = "idx_revoked_expires", columnList = "expires_at")
})
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "jti", nullable = false, unique = true, length = 64)
    private String jti;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}

