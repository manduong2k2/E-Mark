package com.Marketplace_Management.Catalog.DTOs.Requests.Product;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.Marketplace_Management.Catalog.Constants.ProductStatusEnum;
import com.Marketplace_Management.Catalog.DTOs.Commands.Product.CreateProductCommand;
import com.Marketplace_Management.Shared.Annotation.Rules.Uuid;
import com.Marketplace_Management.Shared.Annotation.Rules.Distinct;
import com.Marketplace_Management.Shared.Annotation.Rules.Exist;
import com.Marketplace_Management.Shared.Annotation.Rules.In;
import com.Marketplace_Management.Shared.Annotation.Rules.Unique;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateProductRequest {
    @NotBlank(message = "Product name is required")
    @Size(min = 1, max = 100, message = "Product name must be between 1 and 100 characters")
    private String name;

    @NotNull(message = "Brand ID is required")
    @Exist(table = "brands", column = "id", message = "Brand not found", type = UUID.class)
    @Uuid
    private String brandId;

    @Nullable
    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;

    @Nullable
    @Uuid
    private String vendorId;

    @Nullable
    @Distinct(message = "Each category ID must be unique")
    private List<@Exist(table = "categories", column = "id", message = "Category not found", type = UUID.class) @Uuid String> categoryIds;

    @In(enumClass = ProductStatusEnum.class, message = "Invalid product status")
    private String status;

    @Distinct(field = "optionIds", message = "Variants must have unique option list")
    private List<@Valid CreateProductVariantRequest> variants;

    private List<@Valid CreateProductOptionRequest> options;

    @Data
    public static class CreateProductVariantRequest {
        @NotBlank(message = "Product name is required")
        @Size(min = 1, max = 100, message = "Product name must be between 1 and 100 characters")
        private String name;

        @NotBlank(message = "Product SKU is required")
        @Unique(table = "product_variants", column = "sku", message = "Product variant SKU already exists")
        private String sku;

        @NotNull(message = "Price is required")
        @Min(value = 0, message = "Price must be positive")
        private double price;

        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock must be positive")
        private int stock;

        private List<MultipartFile> images;

        @Distinct
        private List<Long> optionIds;
    }

    @Data
    public static class CreateProductOptionRequest {
        @NotNull(message = "Temporary ID is required")
        private Long tempId;

        @NotBlank(message = "Option name is required")
        @Size(min = 1, max = 100, message = "Option name must be between 1 and 100 characters")
        private String name;

        @NotBlank(message = "Option value is required")
        @Size(min = 1, max = 100, message = "Option value must be between 1 and 100 characters")
        private String value;
    }

    public CreateProductCommand validated() {
        return CreateProductCommand.builder()
                .name(name)
                .brandId(UUID.fromString(brandId))
                .description(description)
                .vendorId(UUID.fromString(vendorId))
                .categoryIds(categoryIds != null ? categoryIds.stream().map(UUID::fromString).toList() : List.of())
                .status(status)
                .variants(variants.stream().map(v -> 
                    CreateProductCommand.CreateProductVariantCommand.builder()
                        .name(v.getName())
                        .sku(v.getSku())
                        .price(v.getPrice())
                        .stock(v.getStock())
                        .images(v.getImages())
                        .optionIds(v.getOptionIds())
                        .build()
                ).toList())
                .options( options != null ? options.stream().map(o -> 
                    CreateProductCommand.CreateProductOptionCommand.builder()
                        .tempId(o.getTempId())
                        .name(o.getName())
                        .value(o.getValue())
                        .build()
                ).toList() : List.of())
                .build();
    }
}