package com.Marketplace_Management.Auth.Contracts;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.Marketplace_Management.Auth.DTOs.Commands.GetListUserCommand;
import com.Marketplace_Management.Auth.DTOs.Response.UserResponse;
import com.Marketplace_Management.Auth.Entities.User;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;

public interface IUserRepository{
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findById(UUID id);
    User save(User user);
    void delete(User user);

    // Admin (Identity & Access)
    PaginatedResponse<UserResponse> findAll(GetListUserCommand command);
    Optional<UserResponse> findResponseById(UUID id);
    List<UUID> findExistingIds(Collection<UUID> ids);
    /** Ids of the (not deleted) users that have this role. */
    List<UUID> findIdsByRole(UUID roleId);
    /** @return number of users that actually got the role (users already having it are skipped) */
    int grantRole(UUID roleId, Collection<UUID> userIds);
    /** @return number of users that actually lost the role */
    int revokeRole(UUID roleId, Collection<UUID> userIds);
}
