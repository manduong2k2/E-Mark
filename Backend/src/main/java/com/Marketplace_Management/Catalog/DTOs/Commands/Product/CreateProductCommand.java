package com.Marketplace_Management.Catalog.DTOs.Commands.Product;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.Marketplace_Management.Shared.DTOs.Commands.BaseCommand;

import lombok.AllArgsConstructor;
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
    private List<CreateProductOptionCommand> options;
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
        private List<Long> optionIds;
    }

    @Data
    @AllArgsConstructor
    @Builder
    @EqualsAndHashCode(callSuper = false)
    public static class CreateProductOptionCommand extends BaseCommand {
        private Long tempId;
        private String name;
        private String value;
    }
}
