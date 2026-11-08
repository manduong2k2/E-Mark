package com.Marketplace_Management.Catalog.Factories;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.Marketplace_Management.Catalog.Constants.ProductStatusEnum;
import com.Marketplace_Management.Catalog.Contracts.IBrandRepository;
import com.Marketplace_Management.Catalog.Contracts.ICategoryRepository;
import com.Marketplace_Management.Catalog.Contracts.IProductFactory;
import com.Marketplace_Management.Catalog.DTOs.Commands.Product.CreateProductCommand;
import com.Marketplace_Management.Catalog.Entities.Brand;
import com.Marketplace_Management.Catalog.Entities.Category;
import com.Marketplace_Management.Catalog.Entities.Product;
import com.Marketplace_Management.Catalog.Entities.ProductAttribute;
import com.Marketplace_Management.Catalog.Entities.ProductAttributeValue;
import com.Marketplace_Management.Catalog.Entities.ProductVariant;
import com.Marketplace_Management.Shared.Contracts.IFileService;
import com.Marketplace_Management.Shared.Entities.File;
import com.Marketplace_Management.Shared.Errors.Exceptions.ResourceNotFoundException;
import com.Marketplace_Management.Shared.Security.SecurityUtils;
import com.Marketplace_Management.Vendor.Contracts.IVendorService;
import com.Marketplace_Management.Vendor.DTOs.Response.VendorResponse;

@Service 
public class ProductFactory implements IProductFactory{
    private final IBrandRepository brandRepository;
    private final ICategoryRepository categoryRepository;
    private final IFileService fileService;
    private final IVendorService vendorService;

    @Value("${spring.application.base-url}")
    private String baseUrl;

    public ProductFactory(IFileService fileService, IBrandRepository brandRepository, 
            ICategoryRepository categoryRepository, IVendorService vendorService) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.fileService = fileService;
        this.vendorService = vendorService;
    }

    public Product create(CreateProductCommand command) throws IOException {
        VendorResponse vendor = command.getVendorId() != null && SecurityUtils.isAdmin()
                ? vendorService.getById(command.getVendorId())
                : vendorService.getByUser(SecurityUtils.currentUserId());
        if (vendor == null) {
            throw new ResourceNotFoundException("Vendor not found");
        }
        
        try {
            Product product = Product.builder()
                    .name(command.getName())
                    .description(command.getDescription())
                    .brand(findBrand(command.getBrandId()))
                    .categories(findCategories(command.getCategoryIds()))
                    .status(command.getStatus() != null ? command.getStatus() : ProductStatusEnum.PUBLISHED.name())
                    .vendorId(vendor.getId())
                    .attributes(command.getAttributes().stream().map(attribute -> {
                        try {
                            String url = fileService.uploadFile(attribute.getIcon(), "catalog/product_variants");
                            return ProductAttribute.builder()
                                    .code(attribute.getCode())
                                    .name(attribute.getName())
                                    .icon(url)
                                    .values(attribute.getValues().stream().map(value -> 
                                        ProductAttributeValue.builder()
                                        .value(value.getValue()).build()).collect(Collectors.toSet())
                                    )
                                    .build();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }).collect(Collectors.toSet()))
                    .variants(command.getVariants().stream().map(variantCommand -> {
                        ProductVariant variant = ProductVariant.builder()
                                .name(variantCommand.getName())
                                .sku(variantCommand.getSku())
                                .price(variantCommand.getPrice())
                                .stock(variantCommand.getStock())
                                .images(new ArrayList<>())
                                .build();
                        if (variantCommand.getImages() != null) {
                            variantCommand.getImages().forEach(imageCommand -> {
                                try {
                                    String url = fileService.uploadFile(imageCommand, "catalog/product_variants");
                                    variant.getImages().add(new File(null, url, variant));
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
                        }
                        return variant;
                    }).collect(Collectors.toCollection(HashSet::new)))
                    .build();
                
                product.getAttributes().forEach(attribute -> {
                    attribute.getValues().forEach(value -> value.setProductAttribute(attribute));
                    attribute.setProduct(product);
                });

                product.getVariants().forEach(variant -> variant.setProduct(product));
                
                return product;
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private Brand findBrand(UUID brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
    }

    private Set<Category> findCategories(List<UUID> categoryIds) {
        Set<Category> categories = new HashSet<>();
        if (categoryIds != null) {
            for (UUID categoryId : categoryIds) {
                categories.add(categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found")));
            }
        }
        return categories;
    }
}
