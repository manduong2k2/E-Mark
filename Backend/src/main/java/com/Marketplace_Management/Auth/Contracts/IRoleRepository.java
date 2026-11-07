package com.Marketplace_Management.Auth.Contracts;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.Marketplace_Management.Auth.DTOs.Commands.GetListRoleCommand;
import com.Marketplace_Management.Auth.DTOs.Response.RoleResponse;
import com.Marketplace_Management.Auth.Entities.RoleEntity;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;

public interface IRoleRepository{
    Optional<RoleEntity> findByCode(String code);
    long count();
    List<RoleEntity> saveAll(List<RoleEntity> roles);

    // Admin (Identity & Access)
    Optional<RoleEntity> findById(UUID id);
    List<RoleEntity> findAllById(Collection<UUID> ids);
    RoleEntity save(RoleEntity role);
    void delete(UUID id);
    PaginatedResponse<RoleResponse> findAll(GetListRoleCommand command);
    Optional<RoleResponse> findResponseById(UUID id);
    long countUsers(UUID roleId);
}
