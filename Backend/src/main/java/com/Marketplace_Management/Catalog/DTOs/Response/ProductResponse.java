package com.Marketplace_Management.Catalog.DTOs.Response;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.Marketplace_Management.Catalog.Entities.ProductEntity;
import com.Marketplace_Management.Vendor.DTOs.Response.VendorResponse;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.Marketplace_Management.Shared.Utils.Helpers.UrlHelper;

@Data
@AllArgsConstructor
@NoArgsConstructor // For Jackson deserialization
@JsonPropertyOrder({ "id", "name", "brand", "categories", "description", "status", "vendorId", "variants" })
public class ProductResponse {
    private UUID id;
    private String name;
    private BrandResponse brand;
    private List<CategoryResponse> categories;
    private String description;
    private String status;
    private List<Variant> variants;
    /** Owning store; vendor (details) is only filled in on create */
    private UUID vendorId;
    private VendorResponse vendor;

    public ProductResponse(ProductEntity product) {
        this(product, true);
    }

    public ProductResponse(ProductEntity product, boolean includeVariants) {
        if (product == null) {
            return;
        }

        this.id = product.getId();
        this.name = product.getName();
        this.brand = product.getBrand() != null ? new BrandResponse(product.getBrand()) : null;
        this.categories = product.getCategories() != null
                ? product.getCategories().stream().map(category -> new CategoryResponse(category)).toList()
                : null;
        this.description = product.getDescription();
        this.status = product.getStatus();
        this.vendorId = product.getVendorId();
        this.variants = includeVariants && product.getVariants() != null ? product.getVariants().stream().map(variant -> Variant.builder()
                .id(variant.getId())
                .name(variant.getName())
                .sku(variant.getSku())
                .stock(variant.getStock())
                .price(variant.getPrice())
                .images(variant.getImages() != null
                        ? variant.getImages().stream()
                                .map(image -> Variant.Image.builder().id(image.getId()).url(image.getUrl()).build())
                                .collect(java.util.stream.Collectors.toSet())
                        : null)
                .build()).toList() : null;
    }

    public ProductResponse withUrl(String url) {
        if (this.getVariants() != null) {
            this.getVariants().forEach(v -> v.withUrl(url));
        }
        if (this.brand != null) {
            this.brand = this.brand.withUrl(url);
        }
        if(this.categories != null) {
            this.categories.forEach(c -> c.withUrl(url));
        }
        return this;
    }

    public ProductResponse withVendor(VendorResponse vendor) {
        this.vendor = vendor;
        return this;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Variant {
        private UUID id;
        private String name;
        private String sku;
        private int stock;
        private double price;
        private Set<Image> images;

        @Data
        @Builder
        private static class Image {
            private UUID id;
            private String url;
        }

        public Variant withUrl(String url) {
            if (this.images != null) {
                this.images.forEach(image -> image.setUrl(UrlHelper.toPublicUrl(url, image.getUrl())));
            }
            return this;
        }
    }
}
