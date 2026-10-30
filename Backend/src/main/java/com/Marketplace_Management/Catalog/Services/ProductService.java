package com.Marketplace_Management.Catalog.Services;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import com.Marketplace_Management.Catalog.Models.ProductOption;
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

    //@Cacheable(value = "products", key = "#command.page + '_' + #command.size + '_' + #command.search + '_' + #command.categoryIds + '_' + #command.brandId")
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
        List<ProductOption> productOptions = command.getOptions().stream().map(option ->
            ProductOption.builder()
                    .id(option.getTempId())
                    .name(option.getName())
                    .value(option.getValue())
                    .build()
        ).collect(java.util.stream.Collectors.toList());

        // Admins may create a product for any vendor; a vendor always creates for its own store.
        // No vendor given: the product belongs to the current user's vendor (getById throws 404 for an unknown id)
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
                .options(new java.util.HashSet<>(productOptions))
                .status(command.getStatus() != null ? command.getStatus() : ProductStatusEnum.PUBLISHED.name())
                .vendorId(command.getVendorId())
                .variants(command.getVariants().stream().map(variant ->
                    ProductVariant.builder()
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
                            .options(productOptions.stream().filter(po -> variant.getOptionIds().contains(po.getId())).collect(java.util.stream.Collectors.toSet()))
                            .build()
                ).collect(java.util.stream.Collectors.toSet()))
                .build();

        productOptions.forEach(option -> {
            option.setProduct(product);
            option.setId(null);
        });

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

        // Map.of rejects null values. Optional update fields such as description,
        // categoryIds and options are allowed to be null by the request DTO.
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put("id", productId);
        updates.put("name", command.getName());
        updates.put("description", command.getDescription());
        updates.put("status", command.getStatus());
        updates.put("categoryIds", command.getCategoryIds());
        updates.put("brandId", command.getBrandId());
        updates.put("variants", command.getVariants() != null ? command.getVariants().stream().map(vc -> 
            ProductVariant.builder()
                    .id(vc.getId())
                    .name(vc.getName())
                    .sku(vc.getSku())
                    .price(vc.getPrice())
                    .stock(vc.getStock())
                    .images(vc.getImages() != null ? vc.getImages().stream().map(img -> {
                        try {
                            String imageUrl = fileService.uploadFile(img, "catalog/product_variants");
                            File file = new File(null, imageUrl);
                            return file;
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to upload file", e);
                        }
                    }).toList() : null)
                    .options((product.getOptions() != null && vc.getOptionIds() != null)
                            ? product.getOptions().stream()
                                .filter(po -> vc.getOptionIds().contains(po.getId()))
                                .collect(java.util.stream.Collectors.toSet())
                            : new java.util.HashSet<>())
                    .build()
        ).collect(java.util.stream.Collectors.toSet()) : null);
        //updates.put("options", command.getOptions());
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
