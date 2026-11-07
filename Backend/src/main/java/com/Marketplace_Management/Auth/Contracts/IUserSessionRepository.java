package com.Marketplace_Management.Auth.Contracts;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.Marketplace_Management.Auth.DTOs.Commands.GetListSessionCommand;
import com.Marketplace_Management.Auth.DTOs.Response.SessionResponse;
import com.Marketplace_Management.Auth.Entities.UserSessionEntity;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;

public interface IUserSessionRepository {
    UserSessionEntity save(UserSessionEntity session);
    List<UserSessionEntity> findByUserIds(Collection<UUID> userIds);
    Optional<UserSessionEntity> findById(UUID id);
    int deleteByRefreshJti(String refreshJti);
    int deleteByUserIdAndAccessJti(UUID userId, String accessJti);
    int deleteByIds(Collection<UUID> ids);
    /** Sessions whose access token has expired: nothing left to revoke. */
    int deleteExpired(Instant now);

    /** Admin: paginated list with embedded user info, optional filters and IP search. */
    PaginatedResponse<SessionResponse> findAll(GetListSessionCommand command);
}
