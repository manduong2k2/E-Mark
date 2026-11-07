package com.Marketplace_Management.Auth.Services;

import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.Marketplace_Management.Auth.Constants.Message;
import com.Marketplace_Management.Auth.Contracts.IRoleRepository;
import com.Marketplace_Management.Auth.Contracts.IRoleService;
import com.Marketplace_Management.Auth.Contracts.IUserRepository;
import com.Marketplace_Management.Auth.DTOs.Commands.GetListRoleCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.GetListUserCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.SaveRoleCommand;
import com.Marketplace_Management.Auth.DTOs.Response.RoleResponse;
import com.Marketplace_Management.Auth.DTOs.Response.UserResponse;
import com.Marketplace_Management.Auth.Events.UserAccessChangedEvent;
import com.Marketplace_Management.Auth.Entities.Role;
import com.Marketplace_Management.Shared.Constants.UserRole;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;
import com.Marketplace_Management.Shared.Errors.Exceptions.BadRequestException;
import com.Marketplace_Management.Shared.Errors.Exceptions.ResourceNotFoundException;

@Service
public class RoleService implements IRoleService {
    private final IRoleRepository roleRepo;
    private final IUserRepository userRepo;
    private final ApplicationEventPublisher eventPublisher;

    public RoleService(IRoleRepository roleRepo, IUserRepository userRepo, ApplicationEventPublisher eventPublisher) {
        this.roleRepo = roleRepo;
        this.userRepo = userRepo;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public PaginatedResponse<RoleResponse> getRoles(GetListRoleCommand command) {
        return roleRepo.findAll(command);
    }

    @Override
    public RoleResponse getRole(UUID id) {
        return roleRepo.findResponseById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Message.ROLE_NOT_FOUND));
    }

    @Override
    public PaginatedResponse<UserResponse> getRoleUsers(GetListUserCommand command) {
        requireRole(command.getRoleId());
        return userRepo.findAll(command);
    }

    // Not @Transactional: the response is read back through QueryBuilder (jOOQ), which uses its own
    // connection and only sees committed rows. Each repository call commits on its own.
    @Override
    public RoleResponse createRole(SaveRoleCommand command) {
        Role saved = roleRepo.save(new Role(null, command.getName(), command.getCode()));
        return getRole(saved.getId());
    }

    // Not @Transactional: the response is read back through QueryBuilder (jOOQ), which uses its own
    // connection and only sees committed rows. Each repository call commits on its own.
    @Override
    // Cached profiles contain role names
    @CacheEvict(value = "userProfiles", allEntries = true)
    public RoleResponse updateRole(UUID id, SaveRoleCommand command) {
        Role role = requireRole(id);
        boolean codeChanged = !role.getCode().equals(command.getCode());

        // System roles are read-only: the code is used by hasAuthority(...) and the admin frontend
        // checks the "Admin" role *name*, so renaming it would lock everyone out of the admin area
        if (UserRole.SYSTEM.contains(role.getCode())) {
            throw new BadRequestException(Message.SYSTEM_ROLE_PROTECTED);
        }
        if (codeChanged) {
            roleRepo.findByCode(command.getCode())
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, Message.ROLE_CODE_EXISTS);
                    });
        }

        role.setName(command.getName());
        role.setCode(command.getCode());
        roleRepo.save(role);
        if (codeChanged) {
            eventPublisher.publishEvent(new UserAccessChangedEvent(userRepo.findIdsByRole(id), "role code changed"));
        }
        return getRole(id);
    }

    @Override
    @Transactional
    public void deleteRole(UUID id) {
        Role role = requireRole(id);

        if (UserRole.SYSTEM.contains(role.getCode())) {
            throw new BadRequestException(Message.SYSTEM_ROLE_PROTECTED);
        }
        // Roles are soft-deleted; refuse while still assigned so users never keep a "deleted" role
        if (roleRepo.countUsers(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, Message.ROLE_HAS_USERS);
        }
        roleRepo.delete(id);
    }

    private Role requireRole(UUID id) {
        return roleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Message.ROLE_NOT_FOUND));
    }
}
