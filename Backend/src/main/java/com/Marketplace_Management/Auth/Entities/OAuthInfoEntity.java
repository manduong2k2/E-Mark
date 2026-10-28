package com.Marketplace_Management.Auth.Entities;

import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.Marketplace_Management.Auth.Constants.OAuthProvider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * A third-party account linked to a user: (provider, provider subject) -> user.
 * The subject is the provider's stable user id ("sub" of the Google ID token, Facebook user id).
 * A user can have one link per provider; the same provider account can belong to only one user.
 */
@Entity
@Table(
    name = "oauth_infos",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_oauth_infos_provider_subject", columnNames = {"oauth_provider", "oauth_provider_subject"}),
    },
    indexes = {
        @Index(name = "idx_oauth_infos_user_id", columnList = "user_id"),
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthInfoEntity {
    @Id
    // UUID v7: time-ordered, so new rows are appended to the primary-key index
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Read-only mapping that only declares the FK (writes go through userId).
    // ON DELETE CASCADE: deleting a user removes their provider links.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_oauth_infos_user"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", nullable = false, length = 20)
    private OAuthProvider oauthProvider;

    @Column(name = "oauth_provider_subject", nullable = false, length = 255)
    private String oauthProviderSubject;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
