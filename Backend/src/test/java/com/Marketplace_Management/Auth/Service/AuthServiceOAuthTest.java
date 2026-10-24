package com.Marketplace_Management.Auth.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;

import com.Marketplace_Management.Auth.Constants.Message;
import com.Marketplace_Management.Auth.Constants.OAuthProvider;
import com.Marketplace_Management.Auth.Constants.UserStatus;
import com.Marketplace_Management.Auth.Contracts.IOAuthInfoRepository;
import com.Marketplace_Management.Auth.Contracts.IUserRepository;
import com.Marketplace_Management.Auth.DTOs.Commands.OAuthLoginCommand;
import com.Marketplace_Management.Auth.DTOs.Response.AuthResponse;
import com.Marketplace_Management.Auth.Models.OAuthInfo;
import com.Marketplace_Management.Auth.Models.User;
import com.Marketplace_Management.Auth.Services.AuthService;
import com.Marketplace_Management.Auth.Services.UserSessionService;
import com.Marketplace_Management.Auth.Services.OAuth.OAuthStrategy;
import com.Marketplace_Management.Auth.Services.OAuth.OAuthUserInfo;
import com.Marketplace_Management.Shared.Errors.Exceptions.BadRequestException;

@ExtendWith(MockitoExtension.class)
class AuthServiceOAuthTest {

    private static final String CREDENTIAL = "provider-token";
    private static final String SUBJECT = "fb-123";
    private static final String EMAIL = "fb@example.com";

    @Mock IUserRepository userRepository;
    @Mock UserSessionService sessionService;
    @Mock Map<String, OAuthStrategy> oauthStrategies;
    @Mock IOAuthInfoRepository oauthInfoRepo;
    @Mock OAuthStrategy facebook;

    @InjectMocks
    AuthService authService;

    private final OAuthLoginCommand command = new OAuthLoginCommand(OAuthProvider.FACEBOOK, CREDENTIAL);

    @BeforeEach
    void setUp() {
        when(oauthStrategies.get("facebook")).thenReturn(facebook);
    }

    private void providerReturns(String email, boolean verified) {
        when(facebook.verify(CREDENTIAL)).thenReturn(
                new OAuthUserInfo(OAuthProvider.FACEBOOK, SUBJECT, email, verified, "FB User", "https://pic"));
    }

    @Test
    void linkedAccount_logsIn() {
        providerReturns(EMAIL, true);
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email(EMAIL).status(UserStatus.ACTIVE).build();
        AuthResponse expected = org.mockito.Mockito.mock(AuthResponse.class);

        when(oauthInfoRepo.findByProviderAndSubject(OAuthProvider.FACEBOOK, SUBJECT))
                .thenReturn(Optional.of(OAuthInfo.builder().userId(userId).build()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sessionService.issue(user, Message.LOGIN_SUCCESS)).thenReturn(expected);

        assertEquals(expected, authService.loginWithOAuth(command));
        verify(oauthInfoRepo, never()).save(any());
    }

    @Test
    void linkedAccount_deactivated_isRejected() {
        providerReturns(EMAIL, true);
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email(EMAIL).status(UserStatus.INACTIVE).build();

        when(oauthInfoRepo.findByProviderAndSubject(OAuthProvider.FACEBOOK, SUBJECT))
                .thenReturn(Optional.of(OAuthInfo.builder().userId(userId).build()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(AuthenticationException.class, () -> authService.loginWithOAuth(command));
        verify(sessionService, never()).issue(any(), any());
    }

    @Test
    void existingActiveEmail_isLinkedToProvider() {
        providerReturns(EMAIL, true);
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email(EMAIL).status(UserStatus.ACTIVE).build();
        AuthResponse expected = org.mockito.Mockito.mock(AuthResponse.class);

        when(oauthInfoRepo.findByProviderAndSubject(OAuthProvider.FACEBOOK, SUBJECT)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(sessionService.issue(user, Message.LOGIN_SUCCESS)).thenReturn(expected);
        when(expected.isNewUser(false)).thenReturn(expected);

        assertEquals(expected, authService.loginWithOAuth(command));

        ArgumentCaptor<OAuthInfo> link = ArgumentCaptor.forClass(OAuthInfo.class);
        verify(oauthInfoRepo).save(link.capture());
        assertEquals(userId, link.getValue().getUserId());
        assertEquals(OAuthProvider.FACEBOOK, link.getValue().getOauthProvider());
        assertEquals(SUBJECT, link.getValue().getOauthProviderSubject());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(sessionService).issue(user, Message.LOGIN_SUCCESS);
    }

    @Test
    void existingInactiveEmail_isRejectedWithoutLinkingOrIssuingSession() {
        providerReturns(EMAIL, true);
        User user = User.builder().id(UUID.randomUUID()).email(EMAIL).status(UserStatus.INACTIVE).build();

        when(oauthInfoRepo.findByProviderAndSubject(OAuthProvider.FACEBOOK, SUBJECT)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.loginWithOAuth(command));

        assertEquals(Message.ACTIVATION, ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(oauthInfoRepo, never()).save(any());
        verify(sessionService, never()).issue(any(), any());
    }

    @Test
    void missingEmail_isRejected() {
        providerReturns(null, false);
        when(oauthInfoRepo.findByProviderAndSubject(OAuthProvider.FACEBOOK, SUBJECT)).thenReturn(Optional.empty());

        AuthenticationException ex = assertThrows(AuthenticationException.class, () -> authService.loginWithOAuth(command));
        assertEquals(Message.OAUTH_EMAIL_REQUIRED, ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(oauthInfoRepo, never()).save(any());
    }

    @Test
    void unsupportedProvider_isRejected() {
        when(oauthStrategies.get("facebook")).thenReturn(null);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.loginWithOAuth(command));

        assertEquals(Message.OAUTH_PROVIDER_UNSUPPORTED, ex.getMessage());
        verify(oauthInfoRepo, never()).findByProviderAndSubject(any(), any());
        verify(sessionService, never()).issue(any(), any());
    }
}
