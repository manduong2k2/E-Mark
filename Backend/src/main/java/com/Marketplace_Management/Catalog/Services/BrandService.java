package com.Marketplace_Management.Catalog.Services;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.Marketplace_Management.Catalog.Contracts.IBrandRepository;
import com.Marketplace_Management.Catalog.Contracts.IBrandService;
import com.Marketplace_Management.Catalog.DTOs.Commands.Brand.CreateBrandCommand;
import com.Marketplace_Management.Catalog.DTOs.Commands.Brand.GetListBrandCommand;
import com.Marketplace_Management.Catalog.DTOs.Commands.Brand.UpdateBrandCommand;
import com.Marketplace_Management.Catalog.DTOs.Response.BrandResponse;
import com.Marketplace_Management.Catalog.Models.Brand;
import com.Marketplace_Management.Shared.Contracts.IFileService;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;
import com.Marketplace_Management.Shared.Errors.Exceptions.ResourceNotFoundException;
import com.Marketplace_Management.Shared.Events.DomainEventDispatcher;

import jakarta.transaction.Transactional;

@Service
public class BrandService implements IBrandService {
    private static final String BRAND_NOT_FOUND = "Brand not found";

    private final IBrandRepository brandRepository;
    private final DomainEventDispatcher domainEvents;
    private final IFileService fileService;

    @Value("${spring.application.base-url:http://localhost:8080}")
    private String baseUrl;

    public BrandService(IBrandRepository brandRepository, DomainEventDispatcher domainEvents, IFileService fileService) {
        this.brandRepository = brandRepository;
        this.domainEvents = domainEvents;
        this.fileService = fileService;
    }

    public PaginatedResponse<BrandResponse> getAll(GetListBrandCommand command) {
        return brandRepository.findAll(command).map(brand -> brand.withUrl(baseUrl));
    }

    public BrandResponse getBrand(UUID brandId) {
        return brandRepository.findById(brandId)
                .map(brand -> new BrandResponse(brand).withUrl(baseUrl))
                .orElseThrow(() -> new ResourceNotFoundException(BRAND_NOT_FOUND));
    }

    @Transactional
    public BrandResponse createBrand(CreateBrandCommand command) throws java.io.IOException {
        Brand brand = Brand.builder()
                .name(command.getName())
                .description(command.getDescription())
                .build();

        brand.setImage(fileService.replaceFile(command.getImage(), null, "catalog/brands/"));
        brand = brandRepository.save(brand);

        domainEvents.dispatch(brand, "brand.created");

        return new BrandResponse(brand).withUrl(baseUrl);
    }

    @Transactional
    public BrandResponse updateBrand(UUID brandId, UpdateBrandCommand command) throws IOException {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException(BRAND_NOT_FOUND));

        brand.setName(command.getName());
        brand.setDescription(command.getDescription());

        brand.setImage(fileService.replaceFile(command.getImage(), brand.getImage(), "catalog/brands/"));

        brand = brandRepository.save(brand);

        domainEvents.dispatch(brand, "brand.updated");

        return new BrandResponse(brand).withUrl(baseUrl);
    }

    @Transactional
    public void deleteBrand(UUID brandId) {
        brandRepository.delete(brandId);
    }

}
