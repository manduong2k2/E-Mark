package com.Marketplace_Management.Catalog.Contracts;

import java.util.Optional;
import java.util.UUID;

import com.Marketplace_Management.Catalog.DTOs.Commands.Category.GetListCategoryCommand;
import com.Marketplace_Management.Catalog.Entities.Category;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;

public interface ICategoryRepository {
    Category save(Category category);

    PaginatedResponse<Category> findAll(GetListCategoryCommand command);

    Optional<Category> findById(UUID id);

    Optional<Category> findByName(String name);

    Category update(Category category);

    void delete(UUID id);
}
