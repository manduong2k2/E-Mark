package com.Marketplace_Management.Auth.Services;

import jakarta.mail.MessagingException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Marketplace_Management.Auth.Constants.Message;
import com.Marketplace_Management.Auth.Constants.UserStatus;
import com.Marketplace_Management.Auth.Contracts.IAuthService;
import com.Marketplace_Management.Auth.Contracts.IOAuthInfoRepository;
import com.Marketplace_Management.Auth.Contracts.IRoleRepository;
import com.Marketplace_Management.Auth.Contracts.IUserRepository;
import com.Marketplace_Management.Auth.DTOs.Commands.ActivateUserCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.ChangePasswordCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.ForgotPasswordCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.OAuthLoginCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.LoginCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.RefreshTokenCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.RegisterCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.ResetPasswordCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.UpdateProfileCommand;
import com.Marketplace_Management.Auth.DTOs.Response.AuthResponse;
import com.Marketplace_Management.Auth.DTOs.Response.RegisterResponse;
import com.Marketplace_Management.Auth.Events.UserAccessChangedEvent;
import com.Marketplace_Management.Auth.Models.Role;
import com.Marketplace_Management.Auth.Models.OAuthInfo;
import com.Marketplace_Management.Auth.Models.User;
import com.Marketplace_Management.Auth.Services.OAuth.OAuthStrategyResolver;
import com.Marketplace_Management.Auth.Services.OAuth.OAuthUserInfo;
import com.Marketplace_Management.Shared.Constants.UserRole;
import com.Marketplace_Management.Shared.Contracts.IFileService;
import com.Marketplace_Management.Shared.Errors.Exceptions.BadRequestException;
import com.Marketplace_Management.Shared.Errors.Exceptions.ResourceNotFoundException;
import com.Marketplace_Management.Shared.Security.JwtService;
import com.Marketplace_Management.Shared.Utils.Helpers.Helper;

@Service
public class AuthService implements IAuthService {
    private final IFileService fileService;
    private final IUserRepository repo;
    private final IRoleRepository roleRepo;
    private final JwtService tokenService;
    private final PasswordEncoder encoder;
    private final EmailVerificationTokenService emailVerificationTokenService;
    private final OAuthStrategyResolver oauthStrategies;
    private final IOAuthInfoRepository oauthInfoRepo;
    private final UserSessionService sessionService;
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(IUserRepository repo, IRoleRepository roleRepo, JwtService tokenService,
            PasswordEncoder encoder, EmailVerificationTokenService emailVerificationTokenService,
            IFileService fileService,
            OAuthStrategyResolver oauthStrategies, IOAuthInfoRepository oauthInfoRepo,
            UserSessionService sessionService,
            ApplicationEventPublisher eventPublisher) {
        this.repo = repo;
        this.roleRepo = roleRepo;
        this.tokenService = tokenService;
        this.encoder = encoder;
        this.emailVerificationTokenService = emailVerificationTokenService;
        this.fileService = fileService;
        this.oauthStrategies = oauthStrategies;
        this.oauthInfoRepo = oauthInfoRepo;
        this.sessionService = sessionService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RegisterResponse register(RegisterCommand command) throws MessagingException {
        User user = User.builder()
                .email(command.getEmail())
                .password(encoder.encode(command.getPassword()))
                .name(command.getName())
                .phone(command.getPhone())
                .status(UserStatus.DEFAULT)
                .build();

        createUserWithDefaultRole(user);

        return new RegisterResponse(true, Message.ACTIVATION_MAIL_SENT);
    }

    @Transactional
    public AuthResponse loginWithOAuth(OAuthLoginCommand command) {
        OAuthUserInfo info = oauthStrategies.resolve(command.getProvider()).verify(command.getCredential());

        Optional<OAuthInfo> link = oauthInfoRepo.findByProviderAndSubject(info.provider(), info.subject());
        if (link.isEmpty()) {
            OAuthUserResult result = linkOrCreateUser(info);
            return sessionService.issue(result.user(), Message.LOGIN_SUCCESS).isNewUser(result.isNewUser());
        }

        User user = repo.findById(link.get().getUserId())
                .filter(linked -> UserStatus.ACTIVE.equals(linked.getStatus()))
                .orElseThrow(() -> new AuthenticationException(Message.ACTIVATION) {
                });

        return sessionService.issue(user, Message.LOGIN_SUCCESS);
    }

    private OAuthUserResult linkOrCreateUser(OAuthUserInfo info) {

        if (info.email() == null || info.email().isBlank() || !info.emailVerified()) {
            throw new AuthenticationException(Message.OAUTH_EMAIL_REQUIRED) {
            };
        }

        Optional<User> existingUser = repo.findByEmail(info.email());
        boolean isNewUser = existingUser.isEmpty();
        User user = existingUser
                .map(existing -> {
                    if (existing.getStatus().equals(UserStatus.INACTIVE)) {
                        throw new BadRequestException(Message.ACTIVATION);
                    }

                    syncUserInfo(existing, info);
                    return existing;
                })
                .orElseGet(() -> createOAuthUser(info));

        oauthInfoRepo.save(
                OAuthInfo.builder()
                        .userId(user.getId())
                        .oauthProvider(info.provider())
                        .oauthProviderSubject(info.subject())
                        .build());

        return new OAuthUserResult(user, isNewUser);
    }

    public record OAuthUserResult(User user, boolean isNewUser) {
    }

    private User syncUserInfo(User user, OAuthUserInfo info) {
        if ((user.getAvatar() == null || user.getAvatar().isBlank()) && info.picture() != null) {
            user.setAvatar(info.picture());
        }
        return repo.save(user);
    }

    private User createOAuthUser(OAuthUserInfo info) {
        return createUserWithDefaultRole(User.builder()
                .email(info.email())
                .password(encoder.encode(Helper.randomString(32)))
                .name(info.name())
                .avatar(info.picture())
                .status(UserStatus.ACTIVE)
                .build());
    }

    private User createUserWithDefaultRole(User user) {
        Role userRole = this.roleRepo.findByCode(UserRole.USER)
                .orElseThrow(() -> new ResourceNotFoundException(Message.ROLE_NOT_FOUND));

        Set<Role> roles = new java.util.HashSet<Role>();
        roles.add(userRole);
        user.setRoles(roles);

        return this.repo.save(user);
    }

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public AuthResponse activeUser(ActivateUserCommand command) {
        var user = repo.findByEmail(command.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(Message.USER_NOT_FOUND));

        boolean isVerified = emailVerificationTokenService.verify(command.getEmail(), command.getToken());
        if (!isVerified) {
            throw new BadRequestException(Message.TOKEN_INVALID);
        }

        user.setStatus(UserStatus.ACTIVE);
        repo.save(user);
        return sessionService.issue(user, Message.ACTIVATED);
    }

    public AuthResponse login(LoginCommand command) {
        return sessionService.issue(authenticate(command), Message.LOGIN_SUCCESS);
    }

    public AuthResponse loginAdmin(LoginCommand command) {
        User user = authenticate(command);
        if (user.getRoles().stream().noneMatch(role -> UserRole.ADMIN.equals(role.getCode()))) {
            throw new AccessDeniedException(Message.FORBIDDEN) {
            };
        }
        return sessionService.issue(user, Message.LOGIN_SUCCESS);
    }

    // Password first, then status: the activation message must not reveal anything
    // to someone without the password
    private User authenticate(LoginCommand command) {
        User user = repo.findByEmail(command.getEmail())
                .filter(found -> encoder.matches(command.getPassword(), found.getPassword()))
                .orElseThrow(() -> new AuthenticationException(Message.CREDENTIALS) {
                });
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new AuthenticationException(Message.ACTIVATION) {
            };
        }
        return user;
    }

    public AuthResponse refreshToken(RefreshTokenCommand command) {
        // Only refresh tokens (typ=refresh), not revoked (a rotated one is accepted
        // during its grace period)
        var claims = tokenService.verifyRefreshToken(command.getRefreshToken());
        if (claims == null) {
            throw new AuthenticationException(Message.TOKEN_INVALID) {
            };
        }

        // Deleted or deactivated users cannot refresh: their access ends here
        var user = repo.findById(UUID.fromString(claims.getSubject()))
                .filter(u -> UserStatus.ACTIVE.equals(u.getStatus()))
                .orElseThrow(() -> new AuthenticationException(Message.TOKEN_INVALID) {
                });

        sessionService.rotate(claims);

        return sessionService.issue(user, Message.TOKEN_REFRESHED);
    }

    public void sendResetPasswordEmail(ForgotPasswordCommand command) throws MessagingException {
        String token = Helper.randomString(16);
        emailVerificationTokenService.createToken(command.getEmail(), token);
        emailVerificationTokenService.sendResetPasswordEmail(command.getEmail(), token);
    }

    // The users cache is keyed by user id (getUserById), which is only known after
    // the email lookup
    @CacheEvict(value = "users", allEntries = true)
    public boolean resetPassword(ResetPasswordCommand command) {
        var user = repo.findByEmail(command.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(Message.USER_NOT_FOUND));

        boolean isVerified = emailVerificationTokenService.verify(command.getEmail(), command.getToken());
        if (!isVerified) {
            throw new BadRequestException(Message.TOKEN_INVALID);
        }

        user.setPassword(encoder.encode(command.getNewPassword()));
        repo.save(user);
        eventPublisher.publishEvent(UserAccessChangedEvent.of(user.getId(), "password reset"));
        return true;
    }

    /**
     * Self-service password change. Signs the user out everywhere else: every
     * session (this one included)
     * is revoked, then a fresh session is issued for the current browser.
     */
    @CacheEvict(value = "users", key = "#userId")
    public AuthResponse changePassword(UUID userId, ChangePasswordCommand command) {
        User user = requireUser(userId);

        if (!encoder.matches(command.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException(Message.CURRENT_PASSWORD_INCORRECT);
        }
        if (encoder.matches(command.getNewPassword(), user.getPassword())) {
            throw new BadRequestException(Message.PASSWORD_SAME_AS_CURRENT);
        }

        user.setPassword(encoder.encode(command.getNewPassword()));
        repo.save(user);

        sessionService.revokeAll(List.of(userId));
        return sessionService.issue(user, Message.PASSWORD_UPDATED);
    }

    public void logout(String accessToken, String refreshToken) {
        sessionService.logout(accessToken, refreshToken);
    }

    @Cacheable(value = "users", key = "#userId")
    public User getUserById(UUID userId) {
        return requireUser(userId);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void updateProfile(UUID userId, UpdateProfileCommand command) throws IOException {
        User user = requireUser(userId);
        if (command.getName() != null && !command.getName().isBlank()) {
            user.setName(command.getName().trim());
        }
        String phone = command.getPhone() != null ? command.getPhone().trim() : "";
        if (!phone.isEmpty() && !phone.equals(user.getPhone())) {
            boolean taken = repo.findByPhone(phone).filter(other -> !other.getId().equals(userId)).isPresent();
            if (taken) {
                throw new BadRequestException(Message.PHONE_EXISTS);
            }
            user.setPhone(phone);
        }
        if (command.getAvatar() != null) {
            String url = fileService.uploadFile(command.getAvatar(), "users/avatars");
            user.setAvatar(url);
        }
        repo.save(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void grantRole(UUID userId, String role) {
        User user = requireUser(userId);

        var roleEntity = roleRepo.findByCode(role)
                .orElseThrow(() -> new ResourceNotFoundException(Message.ROLE_NOT_FOUND));
        user.getRoles().add(roleEntity);
        repo.save(user);
        eventPublisher.publishEvent(UserAccessChangedEvent.of(userId, "role granted: " + role));
    }

    private User requireUser(UUID userId) {
        return repo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(Message.USER_NOT_FOUND));
    }
}
