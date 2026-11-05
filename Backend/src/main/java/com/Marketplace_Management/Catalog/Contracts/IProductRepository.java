package com.Marketplace_Management.Catalog.Contracts;

import java.util.UUID;

import com.Marketplace_Management.Catalog.DTOs.Commands.Product.GetListProductCommand;
import com.Marketplace_Management.Catalog.DTOs.Response.ProductShortResponse;
import com.Marketplace_Management.Catalog.Entities.ProductEntity;
import com.Marketplace_Management.Catalog.Entities.ProductVariantEntity;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;


public interface IProductRepository {
    ProductEntity save(ProductEntity product);

    PaginatedResponse<ProductShortResponse> findAll(GetListProductCommand command);

    ProductEntity findById(UUID id);

    ProductEntity update(ProductEntity product);

    void delete(UUID id);

    ProductVariantEntity findVariantById(UUID variantId);
}
