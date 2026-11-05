package com.Marketplace_Management.Catalog.Services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
import org.springframework.transaction.annotation.Transactional;

import com.Marketplace_Management.Catalog.Constants.ProductStatusEnum;
import com.Marketplace_Management.Catalog.Contracts.IBrandRepository;
import com.Marketplace_Management.Catalog.Contracts.ICategoryRepository;
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
import com.Marketplace_Management.Catalog.Entities.BrandEntity;
import com.Marketplace_Management.Catalog.Entities.CategoryEntity;
import com.Marketplace_Management.Catalog.Entities.FileEntity;
import com.Marketplace_Management.Catalog.Entities.ProductEntity;
import com.Marketplace_Management.Catalog.Entities.ProductVariantEntity;
import com.Marketplace_Management.Shared.Configuration.RabbitMqQueues.ProductQueueConfig;
import com.Marketplace_Management.Shared.Contracts.IEventPublisher;
import com.Marketplace_Management.Shared.Contracts.IFileService;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;
import com.Marketplace_Management.Shared.Errors.Exceptions.ResourceNotFoundException;
import com.Marketplace_Management.Shared.Events.EventOptions;
import com.Marketplace_Management.Shared.Security.SecurityUtils;
import com.Marketplace_Management.Vendor.Contracts.IVendorService;
import com.Marketplace_Management.Vendor.DTOs.Response.VendorResponse;

@Service
public class ProductService implements IProductService {
    private final IProductRepository productRepository;
    private final IBrandRepository brandRepository;
    private final ICategoryRepository categoryRepository;
    private final IEventPublisher eventPublisher;
    private final IFileService fileService;
    private final IVendorService vendorService;

    @Value("${spring.application.base-url}")
    private String baseUrl;

    public ProductService(IProductRepository productRepository, IEventPublisher eventPublisher,
            IFileService fileService, IVendorService vendorService,
            IBrandRepository brandRepository, ICategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.eventPublisher = eventPublisher;
        this.fileService = fileService;
        this.vendorService = vendorService;
    }

    // @Cacheable(value = "products", key = "#command.page + '_' + #command.size +
    // '_' + #command.search + '_' + #command.categoryIds + '_' + #command.brandId")
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ProductShortResponse> getAll(GetListProductCommand command) {

        return productRepository.findAll(command).map(product -> product.withUrl(baseUrl));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "product", key = "#productId")
    public ProductResponse getById(UUID productId) {
        ProductEntity product = productRepository.findById(productId);

        if (product == null) {
            throw new ResourceNotFoundException("Product not found");
        }

        ProductResponse response = new ProductResponse(product).withUrl(baseUrl);
        VendorResponse vendor = vendorService.getById(product.getVendorId());
        response.setVendor(vendor);

        return response;
    }

    @Override
    @Transactional(rollbackFor = IOException.class)
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
        ProductEntity product = new ProductEntity();
        product.setName(command.getName());
        product.setDescription(command.getDescription());
        product.setBrand(findBrand(command.getBrandId()));
        product.setCategories(findCategories(command.getCategoryIds()));
        product.setStatus(command.getStatus() != null ? command.getStatus() : ProductStatusEnum.PUBLISHED.name());
        product.setVendorId(vendor.getId());
        product.setVariants(new HashSet<>());

        for (var variantCommand : command.getVariants()) {
            ProductVariantEntity variant = ProductVariantEntity.builder()
                    .name(variantCommand.getName())
                    .sku(variantCommand.getSku())
                    .price(variantCommand.getPrice())
                    .stock(variantCommand.getStock())
                    .product(product)
                    .images(new ArrayList<>())
                    .build();
            if (variantCommand.getImages() != null) {
                for (var image : variantCommand.getImages()) {
                    String url = fileService.uploadFile(image, "catalog/product_variants");
                    variant.getImages().add(new FileEntity(null, url, variant));
                }
            }
            product.getVariants().add(variant);
        }

        ProductEntity savedProduct = productRepository.save(product);

        return new ProductResponse(savedProduct).withUrl(baseUrl).withVendor(vendor);
    }

    @Override
    @Transactional(rollbackFor = IOException.class)
    @CacheEvict(value = "product", key = "#productId")
    public ProductResponse updateProduct(UUID productId, UpdateProductCommand command) throws IOException {
        ProductEntity product = productRepository.findById(productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product not found");
        }

        product.setName(command.getName());
        product.setDescription(command.getDescription());
        product.setBrand(findBrand(command.getBrandId()));
        if (command.getStatus() != null) {
            product.setStatus(command.getStatus());
        }

        Set<CategoryEntity> categories = findCategories(command.getCategoryIds());
        if (product.getCategories() == null) {
            product.setCategories(categories);
        } else {
            product.getCategories().clear();
            product.getCategories().addAll(categories);
        }
        if (command.getVariants() != null) {
            updateVariants(product, command.getVariants());
        }

        ProductEntity savedProduct = productRepository.update(product);

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
    @Transactional(readOnly = true)
    public ProductVariantResponse getProductVariant(UUID productVariantId) {
        ProductVariantEntity productVariant = productRepository.findVariantById(productVariantId);
        return productVariant != null ? new ProductVariantResponse(productVariant).withUrl(baseUrl) : null;
    }

    private BrandEntity findBrand(UUID brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
    }

    private Set<CategoryEntity> findCategories(List<UUID> categoryIds) {
        Set<CategoryEntity> categories = new HashSet<>();
        if (categoryIds != null) {
            for (UUID categoryId : categoryIds) {
                categories.add(categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found")));
            }
        }
        return categories;
    }

    private void updateVariants(ProductEntity product,
            List<UpdateProductCommand.UpdateProductVariantCommand> commands) throws IOException {
        if (product.getVariants() == null) {
            product.setVariants(new HashSet<>());
        }
        Map<UUID, ProductVariantEntity> existingVariants = product.getVariants().stream()
                .filter(variant -> variant.getId() != null)
                .collect(Collectors.toMap(ProductVariantEntity::getId, variant -> variant));
        Set<UUID> retainedIds = new HashSet<>();
        List<ProductVariantEntity> newVariants = new ArrayList<>();

        for (var command : commands) {
            ProductVariantEntity variant;
            if (command.getId() == null) {
                variant = ProductVariantEntity.builder().product(product).images(new ArrayList<>()).build();
                newVariants.add(variant);
            } else {
                variant = existingVariants.get(command.getId());
                if (variant == null) {
                    throw new IllegalArgumentException("Variant not found");
                }
                if (!retainedIds.add(command.getId())) {
                    throw new IllegalArgumentException("Duplicate variant ID");
                }
            }
            variant.setName(command.getName());
            variant.setSku(command.getSku());
            variant.setPrice(command.getPrice());
            variant.setStock(command.getStock());
            updateImages(variant, command.getImages());
        }

        // Keep existing entities and their attribute selections; remove only omitted variants.
        product.getVariants().removeIf(variant -> !retainedIds.contains(variant.getId()));
        product.getVariants().addAll(newVariants);
    }

    private void updateImages(ProductVariantEntity variant,
            List<UpdateProductCommand.UpdateFileCommand> commands) throws IOException {
        if (commands == null) {
            return;
        }
        if (variant.getImages() == null) {
            variant.setImages(new ArrayList<>());
        }
        Set<UUID> retainedIds = commands.stream().map(UpdateProductCommand.UpdateFileCommand::getId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        variant.getImages().removeIf(image -> !retainedIds.contains(image.getId()));

        for (var command : commands) {
            if (command.getId() == null) {
                String url = fileService.uploadFile(command.getFile(), "catalog/product_variants");
                variant.getImages().add(new FileEntity(null, url, variant));
            } else if (variant.getImages().stream().noneMatch(image -> command.getId().equals(image.getId()))) {
                throw new IllegalArgumentException("Image not found in variant: " + command.getId());
            }
        }
    }

}
