package com.Marketplace_Management.Auth.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.Marketplace_Management.Auth.Contracts.IUserSessionRepository;
import com.Marketplace_Management.Auth.DTOs.Response.AuthResponse;
import com.Marketplace_Management.Auth.Entities.User;
import com.Marketplace_Management.Auth.Entities.Role;
import com.Marketplace_Management.Auth.Entities.UserSession;
import com.Marketplace_Management.Auth.Services.UserSessionService;
import com.Marketplace_Management.Shared.Security.JwtService;
import com.Marketplace_Management.Shared.Security.JwtService.IssuedToken;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@ExtendWith(MockitoExtension.class)
class UserSessionServiceTest {

    @Mock
    JwtService jwtService;

    @Mock
    IUserSessionRepository sessionRepo;

    @InjectMocks
    UserSessionService sessionService;

    @Test
    void issue_returnsTokensAndStoresSessionWithJtisAndExpiries() {
        User user = User.builder().id(UUID.randomUUID()).email("a@b.c")
                .roles(Set.of(new Role(UUID.randomUUID(), "User", "USER"))).build();
        Instant accessExp = Instant.now().plusSeconds(3600);
        Instant refreshExp = Instant.now().plusSeconds(7200);
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn(new IssuedToken("access-token", "access-jti", accessExp));
        when(jwtService.generateRefreshToken(any())).thenReturn(new IssuedToken("refresh-token", "refresh-jti", refreshExp));

        AuthResponse response = sessionService.issue(user, "ok");

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());

        ArgumentCaptor<UserSession> saved = ArgumentCaptor.forClass(UserSession.class);
        verify(sessionRepo).save(saved.capture());
        assertSame(user, saved.getValue().getUser());
        verify(jwtService).generateAccessToken(user.getId(), List.of("USER"), user.getName());
        assertEquals("access-jti", saved.getValue().getAccessJti());
        assertEquals(accessExp, saved.getValue().getAccessExpiresAt());
        assertEquals("refresh-jti", saved.getValue().getRefreshJti());
        assertEquals(refreshExp, saved.getValue().getRefreshExpiresAt());
    }

    @Test
    void rotate_deletesOldSessionAndBlacklistsOldRefreshTokenWithGrace() {
        // JWT "exp" has second precision
        Instant exp = Instant.now().plusSeconds(3600).truncatedTo(ChronoUnit.SECONDS);
        Claims claims = Jwts.claims().setId("old-refresh-jti");
        claims.setExpiration(Date.from(exp));

        sessionService.rotate(claims);

        verify(sessionRepo).deleteByRefreshJti("old-refresh-jti");
        verify(jwtService).blacklistWithGrace("old-refresh-jti", exp);
    }

    @Test
    void revokeAll_deletesOnlySessionsWhoseTokenWasBlacklisted() {
        UUID userId = UUID.randomUUID();
        Instant exp = Instant.now().plusSeconds(3600);
        UserSession ok = UserSession.builder().id(UUID.randomUUID()).user(User.builder().id(userId).build()).accessJti("jti-ok").accessExpiresAt(exp).build();
        UserSession failed = UserSession.builder().id(UUID.randomUUID()).user(User.builder().id(userId).build()).accessJti("jti-failed").accessExpiresAt(exp).build();

        when(sessionRepo.findByUserIds(List.of(userId))).thenReturn(List.of(ok, failed));
        when(jwtService.blacklist("jti-ok", exp)).thenReturn(true);
        when(jwtService.blacklist("jti-failed", exp)).thenReturn(false);   // e.g. Redis down

        int revoked = sessionService.revokeAll(List.of(userId));

        assertEquals(1, revoked);
        // The failed one is kept so it can still be revoked later
        verify(sessionRepo).deleteByIds(List.of(ok.getId()));
    }

    @Test
    void revokeAll_withoutSessions_revokesNothing() {
        UUID userId = UUID.randomUUID();
        when(sessionRepo.findByUserIds(List.of(userId))).thenReturn(List.of());

        assertEquals(0, sessionService.revokeAll(List.of(userId)));
        verify(jwtService, never()).blacklist(any(), any());
    }

    @Test
    void revokeAllBeforeDelete_blacklistsAndDeletesEverySession() {
        UUID userId = UUID.randomUUID();
        Instant exp = Instant.now().plusSeconds(3600);
        UserSession a = UserSession.builder().id(UUID.randomUUID()).user(User.builder().id(userId).build()).accessJti("jti-a").accessExpiresAt(exp).build();
        UserSession b = UserSession.builder().id(UUID.randomUUID()).user(User.builder().id(userId).build()).accessJti("jti-b").accessExpiresAt(exp).build();

        when(sessionRepo.findByUserIds(List.of(userId))).thenReturn(List.of(a, b));
        when(jwtService.blacklist(any(), any())).thenReturn(true);

        sessionService.revokeAllBeforeDelete(userId);

        verify(sessionRepo).deleteByIds(List.of(a.getId(), b.getId()));
    }

    @Test
    void revokeAllBeforeDelete_throwsWhenATokenCannotBeBlacklisted() {
        UUID userId = UUID.randomUUID();
        Instant exp = Instant.now().plusSeconds(3600);
        UserSession s1 = UserSession.builder().id(UUID.randomUUID()).user(User.builder().id(userId).build()).accessJti("jti-1").accessExpiresAt(exp).build();

        when(sessionRepo.findByUserIds(List.of(userId))).thenReturn(List.of(s1));
        when(jwtService.blacklist("jti-1", exp)).thenReturn(false);   // e.g. Redis down

        // Throwing aborts the user delete; otherwise the cascade would drop a still-valid session
        assertThrows(ResponseStatusException.class, () -> sessionService.revokeAllBeforeDelete(userId));
    }
}
