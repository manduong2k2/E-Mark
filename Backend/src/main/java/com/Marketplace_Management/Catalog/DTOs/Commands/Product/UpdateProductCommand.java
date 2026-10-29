package com.Marketplace_Management.Catalog.DTOs.Commands.Product;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.Marketplace_Management.Shared.DTOs.Commands.BaseCommand;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class UpdateProductCommand extends BaseCommand {
    private String name;
    private UUID brandId;
    private String description;
    private List<UUID> categoryIds;
    private List<UpdateProductVariantCommand> variants;
    private List<UpdateProductOptionCommand> options;
    private String status;

    @Data
    @Builder
    @EqualsAndHashCode(callSuper = true)
    public static class UpdateProductVariantCommand extends BaseCommand {
        private UUID id;
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
    public static class UpdateProductOptionCommand extends BaseCommand {
        private UUID id;
        private Long tempId;
        private String name;
        private String value;
    }
}
