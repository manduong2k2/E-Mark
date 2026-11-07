package com.Marketplace_Management.Catalog.Contracts;

import java.util.UUID;

import com.Marketplace_Management.Catalog.DTOs.Commands.Product.GetListProductCommand;
import com.Marketplace_Management.Catalog.DTOs.Response.ProductShortResponse;
import com.Marketplace_Management.Catalog.Entities.Product;
import com.Marketplace_Management.Catalog.Entities.ProductVariant;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;


public interface IProductRepository {
    Product save(Product product);

    PaginatedResponse<ProductShortResponse> findAll(GetListProductCommand command);

    Product findById(UUID id);

    Product update(Product product);

    void delete(UUID id);

    ProductVariant findVariantById(UUID variantId);
}
