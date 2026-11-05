package com.Marketplace_Management.Catalog.Mappers;

import org.springframework.stereotype.Component;

import com.Marketplace_Management.Catalog.Entities.ProductVariantEntity;
import com.Marketplace_Management.Catalog.Models.Product;
import com.Marketplace_Management.Catalog.Models.ProductVariant;
import com.Marketplace_Management.Shared.Contracts.EntityDomainMapper;
import com.Marketplace_Management.Catalog.Entities.FileEntity;
import com.Marketplace_Management.Catalog.Models.File;

@Component
public class ProductVariantMapper implements EntityDomainMapper<ProductVariant, ProductVariantEntity>{

    private final EntityDomainMapper<File, FileEntity> fileMapper;

    public ProductVariantMapper(EntityDomainMapper<File, FileEntity> fileMapper) {
        this.fileMapper = fileMapper;
    }

    public ProductVariant toDomain(ProductVariantEntity entity) {

        if (entity == null) {
            return null;
        }

        return ProductVariant.builder()
            .id(entity.getId())
            .productId(entity.getProduct().getId())
            .name(entity.getName())
            .sku(entity.getSku())
            .price(entity.getPrice())
            .stock(entity.getStock())
            .images(entity.getImages() != null ? entity.getImages().stream().map(fileMapper::toDomain).toList() : null)
            .product(Product.builder()
                .id(entity.getProduct().getId())
                .name(entity.getProduct().getName())
                .description(entity.getProduct().getDescription())
                .brandId(entity.getProduct().getBrand().getId())
                .status(entity.getProduct().getStatus())
                .categoryIds(entity.getProduct().getCategories().stream().map(category -> category.getId()).toList())
                .build()
            )
            .build();
    }

    public ProductVariantEntity toEntity(ProductVariant domain) {

        if (domain == null) {
            return null;
        }

        return ProductVariantEntity.builder()
            .id(domain.getId())
            .name(domain.getName())
            .sku(domain.getSku())
            .price(domain.getPrice().getValue())
            .stock(domain.getStock())
            .images(domain.getImages() != null ? domain.getImages().stream().map(fileMapper::toEntity).toList() : null)
            .build();
    }
}
