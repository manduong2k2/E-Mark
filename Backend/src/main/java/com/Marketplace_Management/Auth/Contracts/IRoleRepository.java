package com.Marketplace_Management.Auth.Contracts;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.Marketplace_Management.Auth.DTOs.Commands.GetListRoleCommand;
import com.Marketplace_Management.Auth.DTOs.Response.RoleResponse;
import com.Marketplace_Management.Auth.Entities.Role;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;

public interface IRoleRepository{
    Optional<Role> findByCode(String code);
    long count();
    List<Role> saveAll(List<Role> roles);

    // Admin (Identity & Access)
    Optional<Role> findById(UUID id);
    List<Role> findAllById(Collection<UUID> ids);
    Role save(Role role);
    void delete(UUID id);
    PaginatedResponse<RoleResponse> findAll(GetListRoleCommand command);
    Optional<RoleResponse> findResponseById(UUID id);
    long countUsers(UUID roleId);
}
