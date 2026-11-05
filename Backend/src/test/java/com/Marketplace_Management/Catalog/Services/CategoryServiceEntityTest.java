package com.Marketplace_Management.Catalog.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.Marketplace_Management.Catalog.Contracts.ICategoryRepository;
import com.Marketplace_Management.Catalog.DTOs.Commands.Category.UpdateCategoryCommand;
import com.Marketplace_Management.Catalog.Entities.CategoryEntity;
import com.Marketplace_Management.Shared.Contracts.IFileService;

@ExtendWith(MockitoExtension.class)
class CategoryServiceEntityTest {
    @Mock ICategoryRepository repository;
    @Mock IFileService fileService;
    @InjectMocks CategoryService service;

    @Test
    void updateKeepsTheExistingChildrenCollection() throws Exception {
        ReflectionTestUtils.setField(service, "baseUrl", "http://api");
        CategoryEntity category = CategoryEntity.builder().id(UUID.randomUUID()).name("Parent").build();
        CategoryEntity child = CategoryEntity.builder().id(UUID.randomUUID()).name("Child").parent(category).build();
        category.getChildren().add(child);
        var children = category.getChildren();
        when(repository.findById(category.getId())).thenReturn(Optional.of(category));
        when(repository.save(category)).thenReturn(category);

        var response = service.updateCategory(category.getId(), UpdateCategoryCommand.builder().name("Updated").build());

        assertSame(children, category.getChildren());
        assertSame(child, children.get(0));
        assertSame(category, child.getParent());
        assertEquals("Updated", response.getName());
        assertEquals(child.getId(), response.getChildren().get(0).getId());
        assertEquals(category.getId(), response.getChildren().get(0).getParentId());
    }
}
