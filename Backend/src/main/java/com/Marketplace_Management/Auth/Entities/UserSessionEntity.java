package com.Marketplace_Management.Auth.Entities;

import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * One active login (token pair). Tracking only — never used to verify a token.
 * Rows are hard-deleted on logout, refresh (replaced by a new row), access change, and
 * by the daily cleanup once the access token has expired.
 * Does not extend JpaEntity on purpose: no soft delete, no entity lifecycle events.
 */
@Entity
@Table(
    name = "user_sessions",
    indexes = {
        // Revoke all sessions of a user / find one session of a user by access token
        @Index(name = "idx_user_sessions_user_access", columnList = "user_id, access_jti"),
        // Refresh: find the session of the refresh token being rotated
        @Index(name = "uk_user_sessions_refresh_jti", columnList = "refresh_jti", unique = true),
        // Daily cleanup of expired sessions
        @Index(name = "idx_user_sessions_access_expires_at", columnList = "access_expires_at"),
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSessionEntity {
    @Id
    // UUID v7: time-ordered, so new rows are appended to the primary-key index
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    // ON DELETE CASCADE: deleting a user removes their sessions — so tokens must be revoked BEFORE
    // the delete (see UserSessionService.revokeAllBeforeDelete).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_sessions_user"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserEntity user;

    // IPv6 max length is 45
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "login_at", nullable = false)
    private Instant loginAt;

    @Column(name = "access_jti", nullable = false, length = 36)
    private String accessJti;

    @Column(name = "access_expires_at", nullable = false)
    private Instant accessExpiresAt;

    @Column(name = "refresh_jti", nullable = false, length = 36)
    private String refreshJti;

    @Column(name = "refresh_expires_at", nullable = false)
    private Instant refreshExpiresAt;

}
