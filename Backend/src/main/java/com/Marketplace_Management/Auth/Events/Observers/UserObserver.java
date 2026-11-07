package com.Marketplace_Management.Auth.Events.Observers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.Marketplace_Management.Auth.Entities.User;
import com.Marketplace_Management.Auth.Events.UserAccessChangedEvent;
import com.Marketplace_Management.Auth.Services.EmailVerificationTokenService;
import com.Marketplace_Management.Auth.Services.UserSessionService;
import com.Marketplace_Management.Shared.Events.EntityEvent;
import com.Marketplace_Management.Shared.Utils.Helpers.Helper;

import jakarta.mail.MessagingException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserObserver {
    private static final Logger logger = LoggerFactory.getLogger(UserObserver.class);

    private final EmailVerificationTokenService tokenService;
    private final UserSessionService sessionService;

    @Async
    // Skip users created already active (Google sign-in): their email is verified by Google
    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        condition = "#event.type == T(com.Marketplace_Management.Shared.Events.EntityEvent.Type).CREATED"
                + " && #event.entity.status != T(com.Marketplace_Management.Auth.Constants.UserStatus).ACTIVE")
    public void onUserRegistered(EntityEvent<User> event) throws MessagingException {
        String token = Helper.randomString(16);
        tokenService.createToken(event.getEntity().getEmail(), token);  
        tokenService.sendVerifyEmail(event.getEntity().getEmail(), token);
    }

    /**
     * Roles/status changed or user deleted: revoke every live access token of these users
     * (blacklist until each token's own expiry) and drop their sessions. Their next request gets
     * 401, the client refreshes and receives a token with the current roles (or is logged out if
     * the user can no longer refresh).
     *
     * AFTER_COMMIT: revoking before commit would let the client refresh into the OLD roles.
     * Synchronous on purpose: the tokens are revoked before the admin's request returns.
     * fallbackExecution: also runs when the change was saved without a surrounding transaction.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAccessChanged(UserAccessChangedEvent event) {
        if (event.userIds() == null || event.userIds().isEmpty()) {
            return;
        }
        int revoked = sessionService.revokeAll(event.userIds());
        logger.info("Access changed ({}) for {} user(s): revoked {} session(s)", event.reason(), event.userIds().size(), revoked);
    }
}