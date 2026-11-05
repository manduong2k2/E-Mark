package com.Marketplace_Management.Catalog.DTOs.Response;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.Marketplace_Management.Catalog.Entities.CategoryEntity;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.Marketplace_Management.Shared.Utils.Helpers.UrlHelper;

@Data
@AllArgsConstructor
@NoArgsConstructor // For Jackson deserialization
@JsonPropertyOrder({"id", "name", "image", "description"})
public class CategoryResponse {
    private UUID id;
    private String name;
    private String image;
    private String description;
    private UUID parentId;
    private List<CategoryResponse> children;

    public CategoryResponse(CategoryEntity category) {
        this.id = category.getId();
        this.name = category.getName();
        this.image = category.getImage() != null ? category.getImage() : null;
        this.description = category.getDescription();
        this.parentId = category.getParent() != null ? category.getParent().getId() : null;
        this.children = category.getChildren() != null ? category.getChildren().stream()
                .map(child -> new CategoryResponse(child))
                .toList() : null;
    }

    public CategoryResponse withUrl(String url) {
        this.image = UrlHelper.toPublicUrl(url, this.image);
        if (this.children != null) {
            this.children.forEach(child -> child.withUrl(url));
        }
        return this;
    }
}
