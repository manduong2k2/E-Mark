package com.Marketplace_Management.Catalog.Controller;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Marketplace_Management.Catalog.Contracts.ICategoryService;
import com.Marketplace_Management.Catalog.DTOs.Commands.Category.CreateCategoryCommand;
import com.Marketplace_Management.Catalog.DTOs.Commands.Category.GetListCategoryCommand;
import com.Marketplace_Management.Catalog.DTOs.Commands.Category.UpdateCategoryCommand;
import com.Marketplace_Management.Catalog.DTOs.Requests.Category.CreateCategoryRequest;
import com.Marketplace_Management.Catalog.DTOs.Requests.Category.GetListCategoryRequest;
import com.Marketplace_Management.Catalog.DTOs.Requests.Category.UpdateCategoryRequest;
import com.Marketplace_Management.Shared.Constants.UserRole;
import com.Marketplace_Management.Shared.Controllers.BaseController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")
public class CategoryController extends BaseController {
    private final ICategoryService categoryService;

    public CategoryController(ICategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll(@Valid @ModelAttribute GetListCategoryRequest request) {
        return paginatedResponse(categoryService.getAll(GetListCategoryCommand.fromRequest(request)));
    }

    @PreAuthorize("hasAuthority('" + UserRole.ADMIN + "')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @ModelAttribute CreateCategoryRequest request) throws IOException {
        return createdResponse(categoryService.createCategory(CreateCategoryCommand.fromRequest(request)));
    }

    @PreAuthorize("hasAuthority('" + UserRole.ADMIN + "')")
    @GetMapping("/{categoryId}")
    public ResponseEntity<Map<String, Object>> details(@PathVariable UUID categoryId) {
        return objectResponse(categoryService.getById(categoryId));
    }

    @PreAuthorize("hasAuthority('" + UserRole.ADMIN + "')")
    @PutMapping("/{categoryId}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable UUID categoryId,
            @Valid @ModelAttribute UpdateCategoryRequest request) throws IOException {
        return objectResponse(categoryService.updateCategory(categoryId, UpdateCategoryCommand.fromRequest(request)));
    }

    @PreAuthorize("hasAuthority('" + UserRole.ADMIN + "')")
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable UUID categoryId) {
        categoryService.deleteCategory(categoryId);
        return successResponse("Category deleted successfully");
    }
}
