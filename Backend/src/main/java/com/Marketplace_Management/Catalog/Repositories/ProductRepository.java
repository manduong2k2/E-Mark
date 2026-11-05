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
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.EntityMetadataRegistry;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.QueryBuilder;

import tools.jackson.databind.ObjectMapper;


@Repository
public class ProductRepository implements IProductRepository {

    private final ProductJpaRepository jpaRepository;
    private final ProductVariantJpaRepository productVariantJpaRepository;
    private final DSLContext dslContext;
    private final EntityMetadataRegistry metadataRegistry;
    private final ObjectMapper objectMapper;


    public ProductRepository(ProductJpaRepository jpaRepository,
            ProductVariantJpaRepository productVariantJpaRepository,
            DSLContext dslContext,
            EntityMetadataRegistry metadataRegistry,
            ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.productVariantJpaRepository = productVariantJpaRepository;
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

    @Override
    public ProductEntity save(ProductEntity product) {
        return jpaRepository.save(product);
    }

    @Override
    public ProductEntity findById(UUID id) {
        return jpaRepository.findById(id).orElse(null);
    }

    @Override
    public ProductEntity update(ProductEntity product) {
        return save(product);
    }

    @Override
    public void delete(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public ProductVariantEntity findVariantById(UUID variantId) {
        return productVariantJpaRepository.findById(variantId).orElse(null);
    }
}
