package com.Marketplace_Management.Catalog.Contracts;

import java.util.Optional;
import java.util.UUID;

import com.Marketplace_Management.Catalog.DTOs.Commands.Brand.GetListBrandCommand;
import com.Marketplace_Management.Catalog.DTOs.Response.BrandResponse;
import com.Marketplace_Management.Catalog.Entities.BrandEntity;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;

public interface IBrandRepository {
    BrandEntity save(BrandEntity brand);

    PaginatedResponse<BrandResponse> findAll(GetListBrandCommand command);

    Optional<BrandEntity> findById(UUID id);

    Optional<BrandEntity> findByName(String name);

    BrandEntity update(BrandEntity brand);

    void delete(UUID id);
}
