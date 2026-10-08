package com.booking.admin;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** An authenticated admin session, identified by a hashed opaque bearer token. */
@Entity
@Table(name = "admin_session")
public class AdminSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "administrator_id")
    private Administrator administrator;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected AdminSession() {
    }

    public AdminSession(Administrator administrator, String tokenHash, Instant createdAt, Instant expiresAt) {
        this.administrator = administrator;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isValidAt(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt) && administrator.isActive();
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public Administrator getAdministrator() {
        return administrator;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
