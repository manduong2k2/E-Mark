package com.Marketplace_Management.Auth.Services;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.Marketplace_Management.Auth.Constants.Message;
import com.Marketplace_Management.Auth.Constants.UserStatus;
import com.Marketplace_Management.Auth.Contracts.IRoleRepository;
import com.Marketplace_Management.Auth.Contracts.IUserRepository;
import com.Marketplace_Management.Auth.Contracts.IUserService;
import com.Marketplace_Management.Auth.DTOs.Commands.CreateUserCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.GetListUserCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.UpdateUserCommand;
import com.Marketplace_Management.Auth.DTOs.Response.UserResponse;
import com.Marketplace_Management.Auth.Events.UserAccessChangedEvent;
import com.Marketplace_Management.Auth.Entities.Role;
import com.Marketplace_Management.Auth.Entities.User;
import com.Marketplace_Management.Shared.Constants.UserRole;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;
import com.Marketplace_Management.Shared.Errors.Exceptions.BadRequestException;
import com.Marketplace_Management.Shared.Errors.Exceptions.ResourceNotFoundException;

/**
 * Admin user management. Note: roles are also embedded in the JWT, so role changes
 * take effect for a user on their next token refresh / login.
 */
@Service
public class UserService implements IUserService {
    private final IUserRepository userRepo;
    private final IRoleRepository roleRepo;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher eventPublisher;
    private final UserSessionService sessionService;

    public UserService(IUserRepository userRepo, IRoleRepository roleRepo, PasswordEncoder encoder,
            ApplicationEventPublisher eventPublisher, UserSessionService sessionService) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.encoder = encoder;
        this.eventPublisher = eventPublisher;
        this.sessionService = sessionService;
    }

    @Override
    public PaginatedResponse<UserResponse> getUsers(GetListUserCommand command) {
        return userRepo.findAll(command);
    }

    @Override
    public UserResponse getUser(UUID id) {
        return userRepo.findResponseById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Message.USER_NOT_FOUND));
    }

    // Not @Transactional: the response is read back through QueryBuilder (jOOQ), which uses its own
    // connection and only sees committed rows. Each repository call commits on its own.
    @Override
    public UserResponse createUser(CreateUserCommand command) {
        ensurePhoneAvailable(command.getPhone(), null);

        User user = User.builder()
                .email(command.getEmail())
                .password(encoder.encode(command.getPassword()))
                .name(command.getName())
                .phone(command.getPhone())
                .status(command.getStatus())
                .roles(resolveRoles(command.getRoleIds()))
                .build();

        User saved = userRepo.save(user);
        return getUser(saved.getId());
    }

    // Not @Transactional: the response is read back through QueryBuilder (jOOQ), which uses its own
    // connection and only sees committed rows. Each repository call commits on its own.
    @Override
    @CacheEvict(value = "userProfiles", key = "#id")
    public UserResponse updateUser(UUID id, UpdateUserCommand command) {
        User user = requireUser(id);

        ensurePhoneAvailable(command.getPhone(), id);

        boolean deactivated = UserStatus.ACTIVE.equals(user.getStatus()) && !UserStatus.ACTIVE.equals(command.getStatus());
        boolean passwordChanged = command.getPassword() != null;

        user.setName(command.getName());
        user.setPhone(command.getPhone());
        user.setStatus(command.getStatus());
        if (passwordChanged) {
            user.setPassword(encoder.encode(command.getPassword()));
        }

        userRepo.save(user);
        // Deactivated or new password: end every live session of this user
        if (deactivated || passwordChanged) {
            eventPublisher.publishEvent(UserAccessChangedEvent.of(id, deactivated ? "deactivated" : "password changed"));
        }
        return getUser(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userProfiles", key = "#id")
    public void deleteUser(UUID id, UUID currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BadRequestException(Message.CANNOT_DELETE_SELF);
        }
        User user = requireUser(id);
        // Sessions cascade with the user row, so revoke their tokens first (not via the AFTER_COMMIT event)
        sessionService.revokeAllBeforeDelete(id);
        userRepo.delete(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userProfiles", allEntries = true)
    public int grantRole(UUID roleId, List<UUID> userIds) {
        requireRole(roleId);
        Set<UUID> ids = requireUsers(userIds);
        int affected = userRepo.grantRole(roleId, ids);
        eventPublisher.publishEvent(new UserAccessChangedEvent(ids, "role granted"));
        return affected;
    }

    @Override
    @Transactional
    @CacheEvict(value = "userProfiles", allEntries = true)
    public int revokeRole(UUID roleId, List<UUID> userIds, UUID currentUserId) {
        Role role = requireRole(roleId);
        Set<UUID> ids = requireUsers(userIds);

        // Prevent an admin from locking themselves out of the admin area
        if (UserRole.ADMIN.equals(role.getCode()) && ids.contains(currentUserId)) {
            throw new BadRequestException(Message.CANNOT_REVOKE_OWN_ADMIN);
        }
        int affected = userRepo.revokeRole(roleId, ids);
        eventPublisher.publishEvent(new UserAccessChangedEvent(ids, "role revoked"));
        return affected;
    }

    // ===== helpers =====

    private void ensurePhoneAvailable(String phone, UUID currentUserId) {
        if (phone == null) {
            return;
        }
        userRepo.findByPhone(phone)
                .filter(other -> !other.getId().equals(currentUserId))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, Message.PHONE_EXISTS);
                });
    }

    /** Requested roles, or the default USER role when none are given. */
    private Set<Role> resolveRoles(List<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            Role userRole = roleRepo.findByCode(UserRole.USER)
                    .orElseThrow(() -> new ResourceNotFoundException(Message.ROLE_NOT_FOUND));
            return new HashSet<>(Set.of(userRole));
        }

        Set<UUID> ids = new LinkedHashSet<>(roleIds);
        List<Role> roles = roleRepo.findAllById(ids);
        if (roles.size() != ids.size()) {
            throw new ResourceNotFoundException(Message.SOME_ROLES_NOT_FOUND);
        }
        return new HashSet<>(roles);
    }

    private Role requireRole(UUID roleId) {
        return roleRepo.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException(Message.ROLE_NOT_FOUND));
    }

    private Set<UUID> requireUsers(List<UUID> userIds) {
        Set<UUID> ids = new LinkedHashSet<>(userIds);
        if (userRepo.findExistingIds(ids).size() != ids.size()) {
            throw new ResourceNotFoundException(Message.SOME_USERS_NOT_FOUND);
        }
        return ids;
    }

    private User requireUser(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(Message.USER_NOT_FOUND));
    }
}
