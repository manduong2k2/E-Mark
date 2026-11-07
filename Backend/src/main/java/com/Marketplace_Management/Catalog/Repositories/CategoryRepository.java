package com.Marketplace_Management.Catalog.Repositories;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.Marketplace_Management.Catalog.Contracts.ICategoryRepository;
import com.Marketplace_Management.Catalog.DTOs.Commands.Category.GetListCategoryCommand;
import com.Marketplace_Management.Catalog.Entities.Category;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.EntityMetadataRegistry;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.QueryBuilder;

import tools.jackson.databind.ObjectMapper;

@Repository
public class CategoryRepository implements ICategoryRepository {
    private final CategoryJpaRepository jpaRepository;
    private final DSLContext dslContext;
    private final EntityMetadataRegistry metadataRegistry;
    private final ObjectMapper objectMapper;

    public CategoryRepository(CategoryJpaRepository jpaRepository, DSLContext dslContext,
            EntityMetadataRegistry metadataRegistry, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.dslContext = dslContext;
        this.metadataRegistry = metadataRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public PaginatedResponse<Category> findAll(GetListCategoryCommand command) {
        QueryBuilder<Category> queryBuilder = new QueryBuilder<>(dslContext, metadataRegistry, objectMapper);
        queryBuilder.query(Category.class)
                .select("id")
                .when(command.getSearch() != null && !command.getSearch().trim().isEmpty(), query -> {
                    String pattern = "%" + command.getSearch() + "%";
                    query.where("name", "ILIKE", pattern)
                            .orWhere("description", "ILIKE", pattern);
                })
                .when(command.getSortBy() != null && !command.getSortBy().trim().isEmpty(), query -> {
                    query.orderBy(command.getSortBy(), "desc".equalsIgnoreCase(command.getSortOrder()) ? "DESC" : "ASC");
                });

        long total = queryBuilder.count();
        int offset = command.getPage() * command.getSize();
        List<UUID> ids = queryBuilder.get(command.getSize(), offset).stream()
                .map(row -> queryBuilder.to(row, Category.class).getId())
                .toList();

        // Load managed entities so parent/children remain available at every depth.
        // findAllById does not preserve the QueryBuilder sort order.
        Map<UUID, Category> categoriesById = ids.isEmpty() ? Map.of()
                : jpaRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(Category::getId, Function.identity()));
        List<Category> categories = ids.stream().map(categoriesById::get).filter(Objects::nonNull).toList();

        return PaginatedResponse.of(categories, command.getPage(), command.getSize(), total);
    }

    @Override
    public Category save(Category category) {
        return jpaRepository.save(category);
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Category> findByName(String name) {
        return jpaRepository.findByName(name);
    }

    @Override
    public Category update(Category category) {
        return save(category);
    }

    @Override
    public void delete(UUID id) {
        jpaRepository.deleteById(id);
    }
}
