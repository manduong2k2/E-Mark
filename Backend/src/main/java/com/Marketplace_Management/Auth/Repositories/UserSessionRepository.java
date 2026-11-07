package com.Marketplace_Management.Auth.Repositories;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.Marketplace_Management.Auth.Contracts.IUserSessionRepository;
import com.Marketplace_Management.Auth.DTOs.Commands.GetListSessionCommand;
import com.Marketplace_Management.Auth.DTOs.Response.SessionResponse;
import com.Marketplace_Management.Auth.Entities.UserSessionEntity;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.QueryBuilder;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.EntityMetadataRegistry;

import tools.jackson.databind.ObjectMapper;

@Repository
public class UserSessionRepository implements IUserSessionRepository {
    private final UserSessionJpaRepository jpaRepository;
    private final DSLContext dsl;
    private final EntityMetadataRegistry metadataRegistry;
    private final ObjectMapper objectMapper;

    public UserSessionRepository(UserSessionJpaRepository jpaRepository,
                                  DSLContext dsl,
                                  EntityMetadataRegistry metadataRegistry,
                                  ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.dsl = dsl;
        this.metadataRegistry = metadataRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public UserSessionEntity save(UserSessionEntity session) {
        return jpaRepository.save(session);
    }

    @Override
    public Optional<UserSessionEntity> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<UserSessionEntity> findByUserIds(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByUserIdIn(userIds);
    }

    @Override
    @Transactional
    public int deleteByRefreshJti(String refreshJti) {
        return jpaRepository.deleteByRefreshJti(refreshJti);
    }

    @Override
    @Transactional
    public int deleteByUserIdAndAccessJti(UUID userId, String accessJti) {
        return jpaRepository.deleteByUserIdAndAccessJti(userId, accessJti);
    }

    @Override
    @Transactional
    public int deleteByIds(Collection<UUID> ids) {
        return ids.isEmpty() ? 0 : jpaRepository.deleteByIds(ids);
    }

    @Override
    @Transactional
    public int deleteExpired(Instant now) {
        return jpaRepository.deleteExpired(now);
    }

    @Override
    public PaginatedResponse<SessionResponse> findAll(GetListSessionCommand command) {
        QueryBuilder<UserSessionEntity> qb = baseQuery()
                .when(command.getUserId() != null,
                        q -> q.where("user.id", "=", command.getUserId()))
                .when(command.getSearch() != null,
                        q -> q.where("ipAddress", "ILIKE", "%" + command.getSearch() + "%"))
                .when(command.getCreatedFrom() != null,
                        q -> q.where("loginAt", ">=", command.getCreatedFrom()))
                .when(command.getCreatedTo() != null,
                        q -> q.where("loginAt", "<=", command.getCreatedTo()))
                .orderBy(
                        "expireAt".equals(command.getSortBy()) ? "accessExpiresAt" : command.getSortBy(),
                        command.getSortOrder());

        long total = qb.count();
        int offset = command.getPage() * command.getSize();

        List<SessionResponse> data = qb.get(command.getSize(), offset)
                .stream()
                .map(this::toResponse)
                .toList();

        return PaginatedResponse.of(data, command.getPage(), command.getSize(), total);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private QueryBuilder<UserSessionEntity> baseQuery() {
        return new QueryBuilder<UserSessionEntity>(dsl, metadataRegistry, objectMapper)
                .query(UserSessionEntity.class)
                .select("id", "ipAddress", "loginAt", "accessExpiresAt")
                .with("user", user -> user.select("id", "name", "email", "avatar"));
    }

    @SuppressWarnings("unchecked")
    private SessionResponse toResponse(Map<String, Object> row) {
        SessionResponse res = new SessionResponse();
        res.setId(toUUID(row.get("id")));
        res.setIpAddress((String) row.get("ipAddress"));

        Object loginAt = row.get("loginAt");
        if (loginAt instanceof Timestamp i) res.setLoginAt(i);

        Object expireAt = row.get("accessExpiresAt");
        if (expireAt instanceof Timestamp i) res.setExpireAt(i);

        Map<String, Object> user = (Map<String, Object>) row.get("user");
        if (user != null) {
            res.setUserId(toUUID(user.get("id")));
            res.setUserName((String) user.get("name"));
            res.setUserEmail((String) user.get("email"));
            res.setUserAvatar((String) user.get("avatar"));
        }
        return res;
    }

    private UUID toUUID(Object value) {
        if (value == null) return null;
        if (value instanceof UUID u) return u;
        return UUID.fromString(value.toString());
    }

}
