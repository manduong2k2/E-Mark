package com.Marketplace_Management.Catalog.Repositories;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.stream.Collectors;

import com.Marketplace_Management.Catalog.Contracts.IProductRepository;
import com.Marketplace_Management.Catalog.DTOs.Commands.Product.GetListProductCommand;
import com.Marketplace_Management.Catalog.DTOs.Response.ProductShortResponse;
import com.Marketplace_Management.Catalog.Entities.ProductEntity;
import com.Marketplace_Management.Catalog.Entities.ProductVariantEntity;
import com.Marketplace_Management.Catalog.Models.Product;
import com.Marketplace_Management.Catalog.Models.ProductVariant;
import com.Marketplace_Management.Shared.Contracts.EntityDomainMapper;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.EntityMetadataRegistry;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.QueryBuilder;

import tools.jackson.databind.ObjectMapper;


@Repository
public class ProductRepository implements IProductRepository {

    private final ProductJpaRepository jpaRepository;
    private final EntityDomainMapper<Product, ProductEntity> productMapper;
    private final EntityDomainMapper<ProductVariant, ProductVariantEntity> variantMapper;
    private final ProductVariantJpaRepository productVariantJpaRepository;
    private final DSLContext dslContext;
    private final EntityMetadataRegistry metadataRegistry;
    private final ObjectMapper objectMapper;


    public ProductRepository(ProductJpaRepository jpaRepository,
            EntityDomainMapper<Product, ProductEntity> productMapper,
            ProductVariantJpaRepository productVariantJpaRepository,
            EntityDomainMapper<ProductVariant, ProductVariantEntity> variantMapper,
            DSLContext dslContext,
            EntityMetadataRegistry metadataRegistry,
            ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.productMapper = productMapper;
        this.productVariantJpaRepository = productVariantJpaRepository;
        this.variantMapper = variantMapper;
        this.dslContext = dslContext;
        this.metadataRegistry = metadataRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public PaginatedResponse<ProductShortResponse> findAll(GetListProductCommand command) {

        QueryBuilder<?> queryBuilder = new QueryBuilder<>(dslContext, metadataRegistry, objectMapper);
        queryBuilder.query(ProductEntity.class)
                .select("id", "name", "status")
                .with("brand", brand -> {
                    brand.select("id", "name");
                })
                .with("categories", categories -> {
                    categories.select("id", "name");
                })
                .with("variants", variant -> {
                    variant
                            .with("images", image -> {
                                image.select("id", "url");
                            });
                })
                .when(command.getBrandId() != null, query -> {
                    query.where("brand.id", "=", command.getBrandId());
                })
                .when(command.getCategoryIds() != null && !command.getCategoryIds().isEmpty(), query -> {
                    query.where("categories.id", "in", command.getCategoryIds());
                })
                .when(command.getVendorId() != null, query -> {
                    query.where("vendorId", "=", command.getVendorId());
                })
                .when(command.getSearch() != null, query -> {
                    query.where("name", "like", "%" + command.getSearch() + "%")
                            .orWhere("variants.name", "like", "%" + command.getSearch() + "%")
                            .orWhere("brand.name", "like", "%" + command.getSearch() + "%");
                })
                .when(command.getSortBy() != null && command.getSortOrder() != null, query -> {
                    query.orderBy(command.getSortBy(), command.getSortOrder());
                });

        long total = queryBuilder.count();

        // Execute and get structured result with pagination
        int offset = command.getPage() * command.getSize();
        var data = queryBuilder.get(command.getSize(), offset)
                .stream()
                .map(item -> queryBuilder.to(item, ProductShortResponse.class))
                .collect(Collectors.toList());

        return PaginatedResponse.of(
                data,
                command.getPage(),
                command.getSize(),
                total);
    }

    public Product save(Product Product) {
        ProductEntity productEntity = productMapper.toEntity(Product);
        ProductEntity saved = jpaRepository.save(productEntity);
        if (saved != null && saved.getVariants() != null && !saved.getVariants().isEmpty()) {
            saved.getVariants().forEach(
                    variant -> {
                        if (variant != null && variant.getImages() != null && !variant.getImages().isEmpty()) {
                            variant.getImages().forEach(image -> image.setEntity(variant));
                        }
                    });
            saved = jpaRepository.save(saved);
        }

        return productMapper.toDomain(saved);
    }

    public Product findById(UUID id) {
        return jpaRepository.findById(id).map(productMapper::toDomain).orElse(null);
    }

    public Product update(Product Product) {
        ProductEntity productEntity = productMapper.toEntity(Product);
        ProductEntity updated = jpaRepository.save(productEntity);
        if (updated != null && updated.getVariants() != null && !updated.getVariants().isEmpty()) {
            updated.getVariants().forEach(
                    variant -> {
                        if (variant != null && variant.getImages() != null && !variant.getImages().isEmpty()) {
                            variant.getImages().forEach(image -> image.setEntity(variant));
                        }
                    });
            updated = jpaRepository.save(updated);
        }
        return productMapper.toDomain(updated);
    }

    public void delete(UUID id) {
        jpaRepository.deleteById(id);
    }

    public ProductVariant findVariantById(UUID variantId) {
        ProductVariantEntity entity = productVariantJpaRepository.findById(variantId).orElse(null);
        return entity != null ? variantMapper.toDomain(entity) : null;
    }
}
