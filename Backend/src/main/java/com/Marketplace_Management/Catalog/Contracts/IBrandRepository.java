package com.Marketplace_Management.Catalog.Contracts;

import java.util.Optional;
import java.util.UUID;

import com.Marketplace_Management.Catalog.DTOs.Commands.Brand.GetListBrandCommand;
import com.Marketplace_Management.Catalog.DTOs.Response.BrandResponse;
import com.Marketplace_Management.Catalog.Entities.Brand;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;

public interface IBrandRepository {
    Brand save(Brand brand);

    PaginatedResponse<BrandResponse> findAll(GetListBrandCommand command);

    Optional<Brand> findById(UUID id);

    Optional<Brand> findByName(String name);

    Brand update(Brand brand);

    void delete(UUID id);
}
