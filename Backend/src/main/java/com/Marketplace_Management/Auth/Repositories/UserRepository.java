package com.Marketplace_Management.Auth.Repositories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import com.Marketplace_Management.Auth.Constants.OAuthProvider;
import com.Marketplace_Management.Auth.Contracts.IOAuthInfoRepository;
import com.Marketplace_Management.Auth.Contracts.IUserRepository;
import com.Marketplace_Management.Auth.DTOs.Commands.GetListUserCommand;
import com.Marketplace_Management.Auth.DTOs.Response.UserResponse;
import com.Marketplace_Management.Auth.Entities.Role;
import com.Marketplace_Management.Auth.Entities.User;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.EntityMetadataRegistry;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.QueryBuilder;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.QueryResults;

import tools.jackson.databind.ObjectMapper;

@Repository
public class UserRepository implements IUserRepository{
    private static final String USERS = "users";

    private final UserJpaRepository userJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final DSLContext dsl;
    private final EntityMetadataRegistry metadataRegistry;
    private final ObjectMapper objectMapper;
    private final IOAuthInfoRepository oauthInfoRepo;

    public UserRepository(UserJpaRepository userJpaRepository, RoleJpaRepository roleJpaRepository,
            DSLContext dsl,
            EntityMetadataRegistry metadataRegistry, ObjectMapper objectMapper, IOAuthInfoRepository oauthInfoRepo) {
        this.userJpaRepository = userJpaRepository;
        this.roleJpaRepository = roleJpaRepository;
        this.dsl = dsl;
        this.metadataRegistry = metadataRegistry;
        this.objectMapper = objectMapper;
        this.oauthInfoRepo = oauthInfoRepo;
    }

    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email);
    }

    public Optional<User> findByPhone(String phone) {
        return userJpaRepository.findByPhone(phone);
    }

    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id);
    }

    public User save(User user) {
        return userJpaRepository.save(user);
    }

    public void delete(User user) {
        userJpaRepository.deleteById(user.getId());
    }

    // ===== Admin queries (QueryBuilder) =====

    @Override
    public PaginatedResponse<UserResponse> findAll(GetListUserCommand command) {
        QueryBuilder<User> queryBuilder = baseQuery()
                .when(command.getSearch() != null, q -> {
                    String pattern = "%" + command.getSearch() + "%";
                    q.where(column("name").likeIgnoreCase(pattern).or(column("email").likeIgnoreCase(pattern)));
                })
                .when(command.getStatus() != null, q -> q.where("status", "=", command.getStatus()))
                .when(command.getRoleId() != null, q -> q.where(hasRole(command.getRoleId())))
                .orderBy(command.getSortBy(), command.getSortOrder());

        long total = queryBuilder.count();
        int offset = command.getPage() * command.getSize();

        List<UserResponse> data = queryBuilder.get(command.getSize(), offset)
                .stream()
                .map(item -> toResponse(queryBuilder, item))
                .toList();

        withOAuthProviders(data);
        return PaginatedResponse.of(data, command.getPage(), command.getSize(), total);
    }

    @Override
    public Optional<UserResponse> findResponseById(UUID id) {
        QueryBuilder<User> queryBuilder = baseQuery().where("id", "=", id);
        Optional<UserResponse> user = queryBuilder.get().stream().findFirst().map(item -> toResponse(queryBuilder, item));
        user.ifPresent(u -> withOAuthProviders(List.of(u)));
        return user;
    }

    // ===== Role assignment (JPA: keeps the user_roles join table managed by Hibernate) =====

    @Override
    public List<UUID> findExistingIds(Collection<UUID> ids) {
        return userJpaRepository.findAllById(ids).stream().map(User::getId).toList();
    }

    @Override
    public List<UUID> findIdsByRole(UUID roleId) {
        return dsl.select(DSL.field(DSL.name(USERS, "id"), UUID.class))
                .from(DSL.table(DSL.name(USERS)))
                .where(hasRole(roleId))
                .and(DSL.field(DSL.name(USERS, "deleted_at")).isNull())
                .fetch(DSL.field(DSL.name(USERS, "id"), UUID.class));
    }

    @Override
    public int grantRole(UUID roleId, Collection<UUID> userIds) {
        Role role = roleJpaRepository.getReferenceById(roleId);
        List<User> changed = new ArrayList<>();

        for (User user : userJpaRepository.findAllById(userIds)) {
            // Skip users that already have the role
            if (user.getRoles().stream().noneMatch(r -> roleId.equals(r.getId()))) {
                user.getRoles().add(role);
                changed.add(user);
            }
        }

        userJpaRepository.saveAll(changed);
        return changed.size();
    }

    @Override
    public int revokeRole(UUID roleId, Collection<UUID> userIds) {
        List<User> users = userJpaRepository.findAllById(userIds);
        List<User> changed = users.stream()
                .filter(user -> user.getRoles().removeIf(r -> roleId.equals(r.getId())))
                .toList();
        userJpaRepository.saveAll(changed);
        return changed.size();
    }

    // ===== helpers =====

    private QueryBuilder<User> baseQuery() {
        QueryBuilder<User> queryBuilder = new QueryBuilder<>(dsl, metadataRegistry, objectMapper);
        return queryBuilder.query(User.class)
                // Explicit columns: never select the password hash
                .select("id", "email", "name", "avatar", "phone", "status", "createdAt")
                .with("roles", role -> role.select("id", "name", "code"));
    }

    private org.jooq.Field<Object> column(String name) {
        return DSL.field(DSL.name(USERS, name));
    }

    /** users.id IN (SELECT user_id FROM user_roles WHERE role_id = ?): keeps every role of the matched users. */
    static Condition hasRole(UUID roleId) {
        return DSL.field(DSL.name(USERS, "id")).in(
                DSL.select(DSL.field(DSL.name("user_id")))
                        .from(DSL.table(DSL.name("user_roles")))
                        .where(DSL.field(DSL.name("role_id")).eq(roleId)));
    }

    private UserResponse toResponse(QueryBuilder<User> queryBuilder, Map<String, Object> item) {
        return queryBuilder.to(QueryResults.normalize(item), UserResponse.class);
    }

    /** Linked sign-in providers (GOOGLE, FACEBOOK…), one query for the whole page. Never the provider ids. */
    private void withOAuthProviders(List<UserResponse> users) {
        Map<UUID, List<OAuthProvider>> providers =
                oauthInfoRepo.findProvidersByUserIds(users.stream().map(UserResponse::getId).toList());
        users.forEach(u -> u.setOauthProviders(
                providers.getOrDefault(u.getId(), List.of()).stream().map(Enum::name).sorted().toList()));
    }
}
