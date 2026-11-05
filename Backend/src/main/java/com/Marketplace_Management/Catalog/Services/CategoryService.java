package com.Marketplace_Management.Catalog.Services;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Marketplace_Management.Catalog.Contracts.ICategoryRepository;
import com.Marketplace_Management.Catalog.Contracts.ICategoryService;
import com.Marketplace_Management.Catalog.DTOs.Commands.Category.CreateCategoryCommand;
import com.Marketplace_Management.Catalog.DTOs.Commands.Category.GetListCategoryCommand;
import com.Marketplace_Management.Catalog.DTOs.Commands.Category.UpdateCategoryCommand;
import com.Marketplace_Management.Catalog.DTOs.Response.CategoryResponse;
import com.Marketplace_Management.Catalog.Entities.CategoryEntity;
import com.Marketplace_Management.Shared.Contracts.IFileService;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;
import com.Marketplace_Management.Shared.Errors.Exceptions.BadRequestException;
import com.Marketplace_Management.Shared.Errors.Exceptions.ResourceNotFoundException;

@Service
public class CategoryService implements ICategoryService {
    private static final String CATEGORY_NOT_FOUND = "Category not found";

    private final ICategoryRepository categoryRepository;
    private final IFileService fileService;

    @Value("${spring.application.base-url:http://localhost:8080}")
    private String baseUrl;

    public CategoryService(ICategoryRepository categoryRepository, IFileService fileService) {
        this.categoryRepository = categoryRepository;
        this.fileService = fileService;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#command.page + '_' + #command.size + '_' + #command.search")
    public PaginatedResponse<CategoryResponse> getAll(GetListCategoryCommand command) {
        return categoryRepository.findAll(command).map(category -> new CategoryResponse(category).withUrl(baseUrl));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(UUID CategoryId) {
        return categoryRepository.findById(CategoryId)
                .map(category -> new CategoryResponse(category).withUrl(baseUrl))
                .orElseThrow(() -> new ResourceNotFoundException(CATEGORY_NOT_FOUND));
    }

    @Transactional(rollbackFor = IOException.class)
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CreateCategoryCommand command) throws IOException {
        CategoryEntity category = CategoryEntity.builder()
                .name(command.getName())
                .description(command.getDescription())
                .build();

        category.setParent(findParent(command.getParentId()));

        category.setImage(fileService.replaceFile(command.getImage(), null, "catalog/categories/"));
        category = categoryRepository.save(category);

        return new CategoryResponse(category).withUrl(baseUrl);
    }

    @Transactional(rollbackFor = IOException.class)
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(UUID categoryId, UpdateCategoryCommand command) throws IOException {
        CategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(CATEGORY_NOT_FOUND));

        category.setName(command.getName());
        category.setDescription(command.getDescription());
        validateCircularReference(categoryId, command.getParentId());
        category.setParent(findParent(command.getParentId()));

        category.setImage(fileService.replaceFile(command.getImage(), category.getImage(), "catalog/categories/"));

        category = categoryRepository.save(category);

        return new CategoryResponse(category).withUrl(baseUrl);
    }

    private CategoryEntity findParent(UUID parentId) {
        if (parentId == null) {
            return null;
        }
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new BadRequestException("Parent category not found"));
    }

    private void validateCircularReference(UUID categoryId, UUID parentId) {
        UUID ancestorId = parentId;
        while (ancestorId != null) {
            if (ancestorId.equals(categoryId)) {
                throw new BadRequestException("Cannot set parent to its descendants");
            }
            ancestorId = categoryRepository.findById(ancestorId)
                    .map(CategoryEntity::getParent)
                    .map(CategoryEntity::getId)
                    .orElse(null);
        }
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(UUID categoryId) {
        categoryRepository.delete(categoryId);
    }

}
