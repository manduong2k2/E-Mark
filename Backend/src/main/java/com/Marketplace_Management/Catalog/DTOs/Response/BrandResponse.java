package com.Marketplace_Management.Catalog.DTOs.Response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.Marketplace_Management.Catalog.Entities.Brand;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.Marketplace_Management.Shared.Utils.Helpers.UrlHelper;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({"id", "name", "image", "description"})
public class BrandResponse {
    private UUID id;
    private String name;
    private String image;
    private String description;
    private long productsCount = 0;

    public BrandResponse(Brand brand) {
        this.id = brand.getId();
        this.name = brand.getName();
        this.image = brand.getImage() != null ? brand.getImage() : null;
        this.description = brand.getDescription();
    }

    public BrandResponse withUrl(String url) {
        this.image = UrlHelper.toPublicUrl(url, this.image);
        return this;
    }
}
