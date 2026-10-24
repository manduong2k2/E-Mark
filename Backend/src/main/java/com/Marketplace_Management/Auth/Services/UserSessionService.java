package com.Marketplace_Management.Auth.Services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import com.Marketplace_Management.Auth.Constants.Message;
import com.Marketplace_Management.Auth.Contracts.IUserSessionRepository;
import com.Marketplace_Management.Auth.DTOs.Commands.GetListSessionCommand;
import com.Marketplace_Management.Auth.DTOs.Response.AuthResponse;
import com.Marketplace_Management.Auth.DTOs.Response.SessionResponse;
import com.Marketplace_Management.Auth.Models.User;
import com.Marketplace_Management.Auth.Models.UserSession;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;
import com.Marketplace_Management.Shared.Security.JwtService;
import com.Marketplace_Management.Shared.Security.JwtService.IssuedToken;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Issues token pairs and keeps user_sessions in sync. Every place that hands out tokens goes
 * through {@link #issue}, so every live access token can be found (by user) and revoked.
 * Stateless verification is untouched: the table is only read to revoke tokens.
 */
@Service
public class UserSessionService {
    private static final Logger logger = LoggerFactory.getLogger(UserSessionService.class);

    private final JwtService jwtService;
    private final IUserSessionRepository sessionRepo;

    public UserSessionService(JwtService jwtService, IUserSessionRepository sessionRepo) {
        this.jwtService = jwtService;
        this.sessionRepo = sessionRepo;
    }

    public AuthResponse issue(User user, String message) {
        List<String> roleCodes = user.getRoles().stream().map(role -> role.getCode()).toList();
        IssuedToken access = jwtService.generateAccessToken(user.getId(), roleCodes, user.getName());
        IssuedToken refresh = jwtService.generateRefreshToken(user.getId());

        sessionRepo.save(UserSession.builder()
                .userId(user.getId())
                .ipAddress(currentIp())
                .loginAt(Instant.now())
                .accessJti(access.jti())
                .accessExpiresAt(access.expiresAt())
                .refreshJti(refresh.jti())
                .refreshExpiresAt(refresh.expiresAt())
                .build());

        return new AuthResponse(access.token(), refresh.token(), message);
    }

    public void rotate(Claims refreshClaims) {
        sessionRepo.deleteByRefreshJti(refreshClaims.getId());
        jwtService.blacklistWithGrace(refreshClaims.getId(), refreshClaims.getExpiration().toInstant());
    }

    public void logout(String accessToken, String refreshToken) {
        jwtService.invalidateToken(accessToken);
        jwtService.invalidateToken(refreshToken);

        Claims refresh = jwtService.decode(refreshToken);
        if (refresh != null) {
            sessionRepo.deleteByRefreshJti(refresh.getId());
            return;
        }
        Claims access = jwtService.decode(accessToken);
        if (access != null && access.getSubject() != null) {
            sessionRepo.deleteByUserIdAndAccessJti(UUID.fromString(access.getSubject()), access.getId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int revokeAll(Collection<UUID> userIds) {
        List<UserSession> sessions = sessionRepo.findByUserIds(userIds);
        List<UUID> revoked = blacklistAndDelete(sessions);

        if (revoked.size() < sessions.size()) {
            logger.error("Revoked {}/{} sessions for users {}: blacklist failed for the rest (kept for retry)",
                    revoked.size(), sessions.size(), userIds);
        }
        return revoked.size();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllBeforeDelete(UUID userId) {
        List<UserSession> sessions = sessionRepo.findByUserIds(List.of(userId));
        List<UUID> revoked = blacklistAndDelete(sessions);

        if (revoked.size() < sessions.size()) {
            logger.error("Revoked {}/{} sessions of user {} before delete: blacklist failed, delete aborted",
                    revoked.size(), sessions.size(), userId);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, Message.SESSION_REVOKE_FAILED);
        }
    }

    /** Blacklists each access token; deletes (and returns) only the sessions whose token was blacklisted. */
    private List<UUID> blacklistAndDelete(List<UserSession> sessions) {
        List<UUID> revoked = new ArrayList<>();
        for (UserSession session : sessions) {
            if (jwtService.blacklist(session.getAccessJti(), session.getAccessExpiresAt())) {
                revoked.add(session.getId());
            }
        }
        sessionRepo.deleteByIds(revoked);
        return revoked;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void deleteExpiredSessions() {
        int deleted = sessionRepo.deleteExpired(Instant.now());
        logger.info("Deleted {} expired user sessions", deleted);
    }

    public PaginatedResponse<SessionResponse> getSessions(GetListSessionCommand command) {
        return sessionRepo.findAll(command);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void terminateSession(UUID sessionId) {
        UserSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Message.SESSION_NOT_FOUND));

        jwtService.blacklist(session.getAccessJti(), session.getAccessExpiresAt());
        jwtService.blacklist(session.getRefreshJti(), session.getRefreshExpiresAt());
        sessionRepo.deleteByIds(List.of(sessionId));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public int terminateSessions(List<UUID> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) return 0;

        List<UserSession> targets = sessionIds.stream()
                .map(id -> sessionRepo.findById(id))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();

        return blacklistAndDelete(targets).size();
    }

    private String currentIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String realIp = request.getHeader("X-Real-IP");
        String ip = realIp != null && !realIp.isBlank() ? realIp.trim() : request.getRemoteAddr();
        return ip != null && ip.length() > 45 ? ip.substring(0, 45) : ip;
    }
}
