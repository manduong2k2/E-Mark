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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAccessChanged(UserAccessChangedEvent event) {
        if (event.userIds() == null || event.userIds().isEmpty()) {
            return;
        }
        int revoked = sessionService.revokeAll(event.userIds());
        logger.info("Access changed ({}) for {} user(s): revoked {} session(s)", event.reason(), event.userIds().size(), revoked);
    }
}