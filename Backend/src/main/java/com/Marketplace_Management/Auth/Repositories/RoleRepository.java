package com.Marketplace_Management.Auth.Repositories;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import com.Marketplace_Management.Auth.Contracts.IRoleRepository;
import com.Marketplace_Management.Auth.DTOs.Commands.GetListRoleCommand;
import com.Marketplace_Management.Auth.DTOs.Response.RoleResponse;
import com.Marketplace_Management.Auth.Entities.Role;
import com.Marketplace_Management.Shared.Constants.UserRole;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.EntityMetadataRegistry;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.QueryBuilder;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.QueryResults;

import tools.jackson.databind.ObjectMapper;

@Repository
public class RoleRepository implements IRoleRepository {
    private final RoleJpaRepository roleJpaRepository;
    private final DSLContext dsl;
    private final EntityMetadataRegistry metadataRegistry;
    private final ObjectMapper objectMapper;

    public RoleRepository(RoleJpaRepository roleJpaRepository,
            DSLContext dsl, EntityMetadataRegistry metadataRegistry, ObjectMapper objectMapper) {
        this.roleJpaRepository = roleJpaRepository;
        this.dsl = dsl;
        this.metadataRegistry = metadataRegistry;
        this.objectMapper = objectMapper;
    }

    public Optional<Role> findByCode(String code) {
        return roleJpaRepository.findByCode(code);
    }

    public long count() {
        return roleJpaRepository.count();
    }

    public List<Role> saveAll(List<Role> roles) {
        return roleJpaRepository.saveAll(roles);
    }

    // ===== Admin (Identity & Access) =====

    @Override
    public Optional<Role> findById(UUID id) {
        return roleJpaRepository.findById(id);
    }

    @Override
    public List<Role> findAllById(Collection<UUID> ids) {
        return roleJpaRepository.findAllById(ids);
    }

    @Override
    public Role save(Role role) {
        return roleJpaRepository.save(role);
    }

    @Override
    public void delete(UUID id) {
        roleJpaRepository.deleteById(id);
    }

    @Override
    public PaginatedResponse<RoleResponse> findAll(GetListRoleCommand command) {
        QueryBuilder<Role> queryBuilder = baseQuery()
                .when(command.getSearch() != null, q -> {
                    String pattern = "%" + command.getSearch() + "%";
                    q.where(column("name").likeIgnoreCase(pattern).or(column("code").likeIgnoreCase(pattern)));
                })
                .orderBy(command.getSortBy(), command.getSortOrder());

        long total = queryBuilder.count();
        int offset = command.getPage() * command.getSize();

        List<RoleResponse> data = queryBuilder.get(command.getSize(), offset)
                .stream()
                .map(item -> queryBuilder.to(QueryResults.normalize(item), RoleResponse.class))
                .toList();

        withUsersCount(data);
        return PaginatedResponse.of(data, command.getPage(), command.getSize(), total);
    }

    @Override
    public Optional<RoleResponse> findResponseById(UUID id) {
        QueryBuilder<Role> queryBuilder = baseQuery().where("id", "=", id);
        Optional<RoleResponse> role = queryBuilder.get().stream().findFirst()
                .map(item -> queryBuilder.to(QueryResults.normalize(item), RoleResponse.class));
        role.ifPresent(r -> withUsersCount(List.of(r)));
        return role;
    }

    @Override
    public long countUsers(UUID roleId) {
        return countUsersByRole(List.of(roleId)).getOrDefault(roleId, 0L);
    }

    // ===== helpers =====

    private QueryBuilder<Role> baseQuery() {
        QueryBuilder<Role> queryBuilder = new QueryBuilder<>(dsl, metadataRegistry, objectMapper);
        return queryBuilder.query(Role.class).select("id", "name", "code", "createdAt");
    }

    private Field<Object> column(String name) {
        return DSL.field(DSL.name("roles", name));
    }

    private void withUsersCount(List<RoleResponse> roles) {
        if (roles.isEmpty()) {
            return;
        }
        Map<UUID, Long> counts = countUsersByRole(roles.stream().map(RoleResponse::getId).toList());
        roles.forEach(role -> {
            role.setUsersCount(counts.getOrDefault(role.getId(), 0L));
            role.setSystem(UserRole.SYSTEM.contains(role.getCode()));
        });
    }

    /**
     * RoleEntity has no inverse "users" relationship for QueryBuilder.withCount, so count the join table directly:
     * SELECT role_id, COUNT(*) FROM user_roles JOIN users ON ... WHERE users.deleted_at IS NULL GROUP BY role_id
     */
    private Map<UUID, Long> countUsersByRole(Collection<UUID> roleIds) {
        Field<UUID> roleId = DSL.field(DSL.name("ur", "role_id"), UUID.class);
        Field<Integer> total = DSL.count().as("total");

        return dsl.select(roleId, total)
                .from(DSL.table(DSL.name("user_roles")).as("ur"))
                .join(DSL.table(DSL.name("users")).as("u"))
                .on(DSL.field(DSL.name("u", "id")).eq(DSL.field(DSL.name("ur", "user_id"))))
                .where(roleId.in(roleIds))
                .and(DSL.field(DSL.name("u", "deleted_at")).isNull())
                .groupBy(roleId)
                .fetch()
                .stream()
                .collect(Collectors.toMap(r -> r.get(roleId), r -> r.get(total).longValue()));
    }
}
