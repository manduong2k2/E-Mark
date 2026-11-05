package com.Marketplace_Management.Catalog.Mappers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Component;

import com.Marketplace_Management.Catalog.Entities.BrandEntity;
import com.Marketplace_Management.Catalog.Entities.CategoryEntity;
import com.Marketplace_Management.Catalog.Entities.ProductEntity;
import com.Marketplace_Management.Catalog.Entities.ProductVariantEntity;
import com.Marketplace_Management.Catalog.Models.Brand;
import com.Marketplace_Management.Catalog.Models.Category;
import com.Marketplace_Management.Catalog.Models.Product;
import com.Marketplace_Management.Catalog.Models.ProductVariant;
import com.Marketplace_Management.Shared.Contracts.EntityDomainMapper;

@Component
public class ProductMapper implements EntityDomainMapper<Product, ProductEntity> {
    @PersistenceContext
    private EntityManager entityManager;

    private final EntityDomainMapper<Brand, BrandEntity> brandMapper;
    private final EntityDomainMapper<Category, CategoryEntity> categoryMapper;
    private final EntityDomainMapper<ProductVariant, ProductVariantEntity> variantMapper;

    public ProductMapper(EntityDomainMapper<Brand, BrandEntity> brandMapper, EntityDomainMapper<Category, CategoryEntity> categoryMapper,
                        EntityDomainMapper<ProductVariant, ProductVariantEntity> variantMapper) {
        this.brandMapper = brandMapper;
        this.categoryMapper = categoryMapper;
        this.variantMapper = variantMapper;
    }

    @Override
    public Product toDomain(ProductEntity entity) {
        return Product.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .brandId(entity.getBrand().getId())
                .brand(brandMapper.toDomain(entity.getBrand()))
                .status(entity.getStatus())
                .categories(entity.getCategories() != null ? entity.getCategories().stream().map(category -> categoryMapper.toDomain(category))
                        .collect(java.util.stream.Collectors.toSet()) : java.util.Collections.emptySet())
                .categoryIds(
                        entity.getCategories() != null ? entity.getCategories().stream().map(category -> category.getId())
                                .collect(java.util.stream.Collectors.toList()) : java.util.Collections.emptyList())
                .variants(entity.getVariants().stream().map(variantMapper::toDomain).collect(java.util.stream.Collectors.toSet()))
                .vendorId(entity.getVendorId())
                .build();
    }

    public ProductEntity toEntity(Product domain) {
        ProductEntity entity = new ProductEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setBrand(entityManager.find(BrandEntity.class, domain.getBrandId()));
        entity.setCategories(
                domain.getCategoryIds() != null ? domain.getCategoryIds().stream()
                        .map(categoryId -> entityManager.find(CategoryEntity.class, categoryId))
                        .collect(java.util.stream.Collectors.toSet()) : java.util.Collections.emptySet());
        entity.setStatus(domain.getStatus());
        entity.setVariants(
                domain.getVariants() != null ? domain.getVariants().stream().map(variantMapper::toEntity)
                        .peek(variant -> variant.setProduct(entity))
                        .collect(java.util.stream.Collectors.toSet()) : java.util.Collections.emptySet());
        
        entity.setVendorId(domain.getVendorId());
        return entity;
    }
}
