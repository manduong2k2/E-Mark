package com.Marketplace_Management.Auth.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.Marketplace_Management.Auth.Constants.Message;
import com.Marketplace_Management.Auth.Contracts.IUserRepository;
import com.Marketplace_Management.Auth.Contracts.IRoleRepository;
import com.Marketplace_Management.Auth.DTOs.Commands.ChangePasswordCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.LoginCommand;
import com.Marketplace_Management.Shared.Errors.Exceptions.BadRequestException;
import com.Marketplace_Management.Auth.DTOs.Response.AuthResponse;
import com.Marketplace_Management.Auth.DTOs.Response.ProfileResponse;
import com.Marketplace_Management.Auth.Entities.UserEntity;
import com.Marketplace_Management.Auth.Entities.RoleEntity;
import com.Marketplace_Management.Auth.Events.UserAccessChangedEvent;
import com.Marketplace_Management.Auth.Services.AuthService;
import com.Marketplace_Management.Auth.Services.UserSessionService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    IUserRepository userRepository;

    @Mock
    IRoleRepository roleRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    PasswordEncoder passwordEncoder;

    // Token issuing (JWT + user_sessions row) is delegated to UserSessionService
    @Mock
    UserSessionService sessionService;

    @InjectMocks
    AuthService authService;

    @Test
    void testLogin_success() {

        // arrange
        String email = "test@gmail.com";
        String rawPassword = "123456";
        String encodedPassword = "encodedPassword";

        UserEntity user = UserEntity.builder()
                .email(email)
                .password(encodedPassword)
                .status("ACTIVE")
                .build();

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(rawPassword, encodedPassword))
                .thenReturn(true);

        when(sessionService.issue(user, Message.LOGIN_SUCCESS))
                .thenReturn(new AuthResponse("fake-token", "fake-refresh-token", Message.LOGIN_SUCCESS));

        // act
        AuthResponse response = authService.login(
                new LoginCommand(email, rawPassword)
        );

        // assert
        assertEquals("fake-token", response.getAccessToken());
        assertEquals("fake-refresh-token", response.getRefreshToken());
        verify(sessionService).issue(user, Message.LOGIN_SUCCESS);
    }

    @Test
    void changePassword_success_revokesAllSessionsThenIssuesANewOne() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).password("old-hash").status("ACTIVE").build();
        AuthResponse issued = new AuthResponse("new-access", "new-refresh", Message.PASSWORD_UPDATED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-secret", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-secret")).thenReturn("new-hash");
        when(userRepository.save(user)).thenReturn(user);
        when(sessionService.issue(user, Message.PASSWORD_UPDATED)).thenReturn(issued);

        AuthResponse response = authService.changePassword(userId, ChangePasswordCommand.builder()
                .currentPassword("old").newPassword("new-secret").build());

        assertEquals(issued, response);
        assertEquals("new-hash", user.getPassword());
        verify(sessionService).revokeAll(List.of(userId));
    }

    @Test
    void changePassword_wrongCurrentPassword_isRejected() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).password("old-hash").status("ACTIVE").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.changePassword(userId,
                ChangePasswordCommand.builder().currentPassword("wrong").newPassword("new-secret").build()));

        assertEquals(Message.CURRENT_PASSWORD_INCORRECT, ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(sessionService, never()).revokeAll(any());
    }

    @Test
    void profileIsACacheableDtoWithoutPasswordOrEntityReferences() {
        LocalDateTime created = LocalDateTime.of(2026, 1, 1, 12, 0);
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email("profile@example.com")
                .name("Profile").password("secret-hash").status("ACTIVE").createdAt(created)
                .roles(Set.of(new RoleEntity(UUID.randomUUID(), "User", "USER"))).build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        ProfileResponse profile = authService.getUserById(user.getId());
        RedisSerializer<Object> serializer = RedisSerializer.json();
        byte[] serialized = serializer.serialize(profile);
        ProfileResponse restored = (ProfileResponse) serializer.deserialize(serialized);

        assertEquals(user.getId(), restored.getId());
        assertEquals(Set.of("User"), restored.getRoles());
        assertEquals(created, restored.getCreatedAt());
        String json = new String(serialized, StandardCharsets.UTF_8);
        assertFalse(json.contains("secret-hash"));
        assertFalse(json.contains("\"password\""));
        assertFalse(json.contains("UserEntity"));
    }

    @Test
    void adminLoginRequiresTheAdminRoleBeforeIssuingASession() {
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email("user@example.com")
                .password("hash").status("ACTIVE")
                .roles(Set.of(new RoleEntity(UUID.randomUUID(), "User", "USER"))).build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);

        assertThrows(AccessDeniedException.class,
                () -> authService.loginAdmin(new LoginCommand(user.getEmail(), "password")));

        verify(sessionService, never()).issue(any(), any());
    }

    @Test
    void roleGrantKeepsExistingEntityRolesAndPublishesAccessChange() {
        RoleEntity originalRole = new RoleEntity(UUID.randomUUID(), "User", "USER");
        RoleEntity vendorRole = new RoleEntity(UUID.randomUUID(), "Vendor", "VENDOR");
        Set<RoleEntity> roles = new HashSet<>(Set.of(originalRole));
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).roles(roles).build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("VENDOR")).thenReturn(Optional.of(vendorRole));

        authService.grantRole(user.getId(), "VENDOR");

        assertSame(roles, user.getRoles());
        assertEquals(Set.of(originalRole, vendorRole), roles);
        verify(userRepository).save(user);
        var event = ArgumentCaptor.forClass(UserAccessChangedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(Set.of(user.getId()), Set.copyOf(event.getValue().userIds()));
    }
}
