package com.Marketplace_Management.Catalog.Contracts;

import java.io.IOException;
import java.util.UUID;

import com.Marketplace_Management.Catalog.DTOs.Commands.Category.CreateCategoryCommand;
import com.Marketplace_Management.Catalog.DTOs.Commands.Category.GetListCategoryCommand;
import com.Marketplace_Management.Catalog.DTOs.Commands.Category.UpdateCategoryCommand;
import com.Marketplace_Management.Catalog.DTOs.Response.CategoryResponse;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;

public interface ICategoryService {
    public PaginatedResponse<CategoryResponse> getAll(GetListCategoryCommand command);

    public CategoryResponse createCategory(CreateCategoryCommand command) throws IOException;

    public CategoryResponse getById(UUID id);

    public CategoryResponse updateCategory(UUID id, UpdateCategoryCommand command) throws IOException;

    public void deleteCategory(UUID id);

}
