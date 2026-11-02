package com.Marketplace_Management.Catalog.DTOs.Response;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.Marketplace_Management.Catalog.Models.Product;
import com.Marketplace_Management.Vendor.DTOs.Response.VendorResponse;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.Marketplace_Management.Shared.Utils.Helpers.UrlHelper;

@Data
@AllArgsConstructor
@NoArgsConstructor // For Jackson deserialization
@JsonPropertyOrder({ "id", "name", "brand", "categories", "description", "status", "vendorId", "options", "variants" })
public class ProductResponse {
    private UUID id;
    private String name;
    private BrandResponse brand;
    private List<CategoryResponse> categories;
    private String description;
    private String status;
    private List<Variant> variants;
    private List<ProductOptionResponse> options;
    /** Owning store; vendor (details) is only filled in on create */
    private UUID vendorId;
    private VendorResponse vendor;

    public ProductResponse(Product product) {
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
        this.options = product.getOptions() != null
                ? product.getOptions().stream().map(option -> new ProductOptionResponse(option)).toList()
                : null;
        this.variants = product.getVariants() != null ? product.getVariants().stream().map(variant -> Variant.builder()
                .id(variant.getId())
                .name(variant.getName())
                .sku(variant.getSku())
                .stock(variant.getStock())
                .price(variant.getPrice() != null ? variant.getPrice().getValue() : 0.0)
                .optionList(variant.getOptionList())
                .images(variant.getImages() != null
                        ? variant.getImages().stream()
                                .map(image -> Variant.Image.builder().id(image.getId()).url(image.getUrl()).build())
                                .collect(java.util.stream.Collectors.toSet())
                        : null)
                .options(variant.getOptions() != null
                        ? variant.getOptions().stream().map(option -> new ProductOptionResponse(option)).collect(java.util.stream.Collectors.toSet())
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
        private String optionList;
        private Set<Image> images;
        private Set<ProductOptionResponse> options;

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
