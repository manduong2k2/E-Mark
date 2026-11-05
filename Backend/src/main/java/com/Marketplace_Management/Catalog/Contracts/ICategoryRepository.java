package com.Marketplace_Management.Catalog.Contracts;

import java.util.Optional;
import java.util.UUID;

import com.Marketplace_Management.Catalog.DTOs.Commands.Category.GetListCategoryCommand;
import com.Marketplace_Management.Catalog.Entities.CategoryEntity;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;

public interface ICategoryRepository {
    CategoryEntity save(CategoryEntity category);

    PaginatedResponse<CategoryEntity> findAll(GetListCategoryCommand command);

    Optional<CategoryEntity> findById(UUID id);

    Optional<CategoryEntity> findByName(String name);

    CategoryEntity update(CategoryEntity category);

    void delete(UUID id);
}
