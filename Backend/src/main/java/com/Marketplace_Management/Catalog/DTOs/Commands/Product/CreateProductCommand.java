package com.Marketplace_Management.Catalog.DTOs.Commands.Product;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.Marketplace_Management.Shared.DTOs.Commands.BaseCommand;

import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Builder;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class CreateProductCommand extends BaseCommand {
    private String name;
    private UUID brandId;
    private String description;
    private List<UUID> categoryIds;
    private String status;
    private List<CreateProductVariantCommand> variants;
    private List<CreateProductAttributeCommand> attributes;
    private UUID vendorId;

    @Data
    @Builder
    @EqualsAndHashCode(callSuper = true)
    public static class CreateProductVariantCommand extends BaseCommand {
        private String name;
        private String sku;
        private double price;
        private int stock;

        private List<MultipartFile> images;
        private List<Long> attributeValueIds;
    }

    @Data
    @Builder 
    public static class CreateProductAttributeCommand {
        private String code;
        private String name;
        private MultipartFile icon;
        private String description;

        private List<@Valid CreateProductAttributeValueCommand> values;
    }

    @Data
    @Builder 
    public static class CreateProductAttributeValueCommand {
        private long tempId;
        private String value;
    }
}
