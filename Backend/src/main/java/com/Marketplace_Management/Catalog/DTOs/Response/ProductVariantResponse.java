package com.Marketplace_Management.Catalog.DTOs.Response;

import java.util.List;
import java.util.UUID;

import com.Marketplace_Management.Catalog.Entities.ProductVariantEntity;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.Marketplace_Management.Shared.Utils.Helpers.UrlHelper;

@Data
@NoArgsConstructor // For Jackson deserialization
@AllArgsConstructor
@JsonPropertyOrder({"id", "name", "sku", "stock", "price", "images"})
public class ProductVariantResponse {
    private UUID id;
    private String name;
    private String sku;
    private int stock;
    private double price;
    private List<String> images;
    private ProductResponse product;

    public ProductVariantResponse(ProductVariantEntity variant) {
        if (variant == null) {
            return;
        }
        this.id = variant.getId();
        this.name = variant.getName();
        this.sku = variant.getSku();
        this.stock = variant.getStock();
        this.price = variant.getPrice();
        this.images = variant.getImages() != null ? variant.getImages().stream().map(image -> image.getUrl()).toList() : null;
        this.product = variant.getProduct() != null ? new ProductResponse(variant.getProduct(), false) : null;
    }

    public ProductVariantResponse withUrl(String url) {
        this.images = this.images != null ? this.images.stream().map(image -> UrlHelper.toPublicUrl(url, image)).toList() : null;
        if (this.product != null) {
            this.product.withUrl(url);
        }
        return this;
    }
}
