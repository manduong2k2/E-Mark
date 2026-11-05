package com.Marketplace_Management.Catalog.Services;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.Marketplace_Management.Catalog.Constants.ProductStatusEnum;
import com.Marketplace_Management.Catalog.Contracts.IProductRepository;
import com.Marketplace_Management.Catalog.Contracts.IProductService;
import com.Marketplace_Management.Catalog.DTOs.Commands.Product.CreateProductCommand;
import com.Marketplace_Management.Catalog.DTOs.Commands.Product.GetListProductCommand;
import com.Marketplace_Management.Catalog.DTOs.Commands.Product.UpdateProductCommand;
import com.Marketplace_Management.Catalog.DTOs.Response.ProductResponse;
import com.Marketplace_Management.Catalog.DTOs.Response.ProductShortResponse;
import com.Marketplace_Management.Catalog.DTOs.Response.ProductVariantResponse;
import com.Marketplace_Management.Catalog.Events.ProductArchivedEvent;
import com.Marketplace_Management.Catalog.Events.ProductDeletedEvent;
import com.Marketplace_Management.Catalog.Models.File;
import com.Marketplace_Management.Catalog.Models.Product;
import com.Marketplace_Management.Catalog.Models.ProductVariant;
import com.Marketplace_Management.Shared.Configuration.RabbitMqQueues.ProductQueueConfig;
import com.Marketplace_Management.Shared.Contracts.IEventPublisher;
import com.Marketplace_Management.Shared.Contracts.IFileService;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;
import com.Marketplace_Management.Shared.Errors.Exceptions.ResourceNotFoundException;
import com.Marketplace_Management.Shared.Events.EventOptions;
import com.Marketplace_Management.Shared.Security.SecurityUtils;
import com.Marketplace_Management.Vendor.Contracts.IVendorService;
import com.Marketplace_Management.Vendor.DTOs.Response.VendorResponse;

import jakarta.transaction.Transactional;

@Service
public class ProductService implements IProductService {
    private final IProductRepository productRepository;
    private final IEventPublisher eventPublisher;
    private final IFileService fileService;
    private final IVendorService vendorService;

    @Value("${spring.application.base-url}")
    private String baseUrl;

    public ProductService(IProductRepository productRepository, IEventPublisher eventPublisher,
            IFileService fileService, IVendorService vendorService) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
        this.fileService = fileService;
        this.vendorService = vendorService;
    }

    // @Cacheable(value = "products", key = "#command.page + '_' + #command.size +
    // '_' + #command.search + '_' + #command.categoryIds + '_' + #command.brandId")
    @Override
    public PaginatedResponse<ProductShortResponse> getAll(GetListProductCommand command) {

        return productRepository.findAll(command).map(product -> product.withUrl(baseUrl));
    }

    @Override
    @Cacheable(value = "product", key = "#productId")
    public ProductResponse getById(UUID productId) {
        Product product = productRepository.findById(productId);

        if (product == null) {
            throw new ResourceNotFoundException("Product not found");
        }

        ProductResponse response = new ProductResponse(product).withUrl(baseUrl);
        VendorResponse vendor = vendorService.getById(product.getVendorId());
        response.setVendor(vendor);

        return response;
    }

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductCommand command) throws IOException {
        // Admins may create a product for any vendor; a vendor always creates for its
        // own store.
        // No vendor given: the product belongs to the current user's vendor (getById
        // throws 404 for an unknown id)
        VendorResponse vendor = command.getVendorId() != null && SecurityUtils.isAdmin()
                ? vendorService.getById(command.getVendorId())
                : vendorService.getByUser(SecurityUtils.currentUserId());
        if (vendor == null) {
            throw new ResourceNotFoundException("Vendor not found");
        }
        command.setVendorId(vendor.getId());

        Product product = Product.builder()
                .name(command.getName())
                .description(command.getDescription())
                .brandId(command.getBrandId())
                .status(command.getStatus())
                .categoryIds(command.getCategoryIds())
                .status(command.getStatus() != null ? command.getStatus() : ProductStatusEnum.PUBLISHED.name())
                .vendorId(command.getVendorId())
                .variants(command.getVariants().stream().map(variant -> ProductVariant.builder()
                        .name(variant.getName())
                        .sku(variant.getSku())
                        .price(variant.getPrice())
                        .stock(variant.getStock())
                        .images(variant.getImages() != null ? variant.getImages().stream().map(img -> {
                            try {
                                String imageUrl = fileService.uploadFile(img, "catalog/product_variants");
                                File file = new File(null, imageUrl);
                                return file;
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to upload file", e);
                            }
                        }).toList() : null)
                        .build()).collect(java.util.stream.Collectors.toSet()))
                .build();

        Product savedProduct = productRepository.save(product);

        return new ProductResponse(savedProduct).withUrl(baseUrl).withVendor(vendor);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public ProductResponse updateProduct(UUID productId, UpdateProductCommand command) throws IOException {
        Product product = productRepository.findById(productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product not found");
        }

        // Map.of rejects null values. Optional update fields such as description
        // and categoryIds are allowed to be null by the request DTO.
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put("id", productId);
        updates.put("name", command.getName());
        updates.put("description", command.getDescription());
        updates.put("status", command.getStatus());
        updates.put("categoryIds", command.getCategoryIds());
        updates.put("brandId", command.getBrandId());
        updates.put("variants", command.getVariants() != null
                ? command.getVariants().stream().map(vc -> {

                    var variant = product.getVariants().stream()
                            .filter(v -> Objects.equals(v.getId(), vc.getId()))
                            .findFirst()
                            .orElse(null);

                    if (vc.getId() != null && variant == null) {
                        throw new IllegalArgumentException("Variant not found");
                    }

                    List<File> images = new ArrayList<>(
                            variant != null && variant.getImages() != null
                                    ? variant.getImages()
                                    : List.of());

                    if (vc.getImages() != null) {
                        Set<UUID> retainedIds = vc.getImages().stream()
                                .map(img -> img.getId())
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());

                        // Remove images omitted from request
                        images.removeIf(img -> !retainedIds.contains(img.getId()));

                        for (var img : vc.getImages()) {
                            if (img.getId() == null) {
                                try {
                                    String url = fileService.uploadFile(
                                            img.getFile(),
                                            "catalog/product_variants");

                                    images.add(new File(null, url));
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            } else {
                                boolean exists = images.stream()
                                        .anyMatch(existing -> Objects.equals(existing.getId(), img.getId()));

                                if (!exists) {
                                    throw new IllegalArgumentException(
                                            "Image not found in variant: " + img.getId());
                                }
                            }
                        }
                    }

                    return ProductVariant.builder()
                            .id(vc.getId())
                            .name(vc.getName())
                            .sku(vc.getSku())
                            .price(vc.getPrice())
                            .stock(vc.getStock())
                            .images(images)
                            .build();

                }).collect(Collectors.toSet())
                : null);
        product.update(updates);

        Product savedProduct = productRepository.update(product);

        if ("ARCHIVED".equals(product.getStatus())) {
            eventPublisher.publish(
                    new ProductArchivedEvent(product.getId()),
                    new EventOptions(ProductQueueConfig.PRODUCT_ARCHIVED_QUEUE, false));
        }

        return new ProductResponse(savedProduct).withUrl(baseUrl);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public void deleteProduct(UUID productId) {
        productRepository.delete(productId);
        ProductDeletedEvent event = new ProductDeletedEvent(productId);
        eventPublisher.publish(event, new EventOptions(ProductQueueConfig.PRODUCT_DELETED_QUEUE, false));
    }

    @Override
    public List<String> getAllStatus() {
        return Arrays.stream(ProductStatusEnum.values()).map(ProductStatusEnum::name).collect(Collectors.toList());
    }

    @Override
    public ProductVariantResponse getProductVariant(UUID productVariantId) {
        ProductVariant productVariant = productRepository.findVariantById(productVariantId);
        return productVariant != null ? new ProductVariantResponse(productVariant).withUrl(baseUrl) : null;
    }

}
