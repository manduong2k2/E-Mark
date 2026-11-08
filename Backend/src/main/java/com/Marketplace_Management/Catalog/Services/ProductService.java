package com.Marketplace_Management.Catalog.Services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Marketplace_Management.Catalog.Constants.ProductStatusEnum;
import com.Marketplace_Management.Catalog.Contracts.IBrandRepository;
import com.Marketplace_Management.Catalog.Contracts.ICategoryRepository;
import com.Marketplace_Management.Catalog.Contracts.IProductFactory;
import com.Marketplace_Management.Catalog.Contracts.IProductRepository;
import com.Marketplace_Management.Catalog.Contracts.IProductService;
import com.Marketplace_Management.Catalog.DTOs.Commands.Product.CreateProductCommand;
import com.Marketplace_Management.Catalog.DTOs.Commands.Product.GetListProductCommand;
import com.Marketplace_Management.Catalog.DTOs.Commands.Product.UpdateProductCommand;
import com.Marketplace_Management.Catalog.DTOs.Response.ProductResponse;
import com.Marketplace_Management.Catalog.DTOs.Response.ProductShortResponse;
import com.Marketplace_Management.Catalog.DTOs.Response.ProductVariantResponse;
import com.Marketplace_Management.Catalog.Entities.Brand;
import com.Marketplace_Management.Catalog.Entities.Category;
import com.Marketplace_Management.Catalog.Entities.Product;
import com.Marketplace_Management.Catalog.Entities.ProductAttribute;
import com.Marketplace_Management.Catalog.Entities.ProductAttributeValue;
import com.Marketplace_Management.Catalog.Entities.ProductVariant;
import com.Marketplace_Management.Catalog.Events.ProductArchivedEvent;
import com.Marketplace_Management.Catalog.Events.ProductDeletedEvent;
import com.Marketplace_Management.Shared.Configuration.RabbitMqQueues.ProductQueueConfig;
import com.Marketplace_Management.Shared.Contracts.IEventPublisher;
import com.Marketplace_Management.Shared.Contracts.IFileService;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;
import com.Marketplace_Management.Shared.Entities.File;
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
    private final IProductFactory productFactory;

    @Value("${spring.application.base-url}")
    private String baseUrl;

    public ProductService(IProductRepository productRepository, IEventPublisher eventPublisher,
            IFileService fileService, IVendorService vendorService, IProductFactory productFactory,
            IBrandRepository brandRepository, ICategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.eventPublisher = eventPublisher;
        this.fileService = fileService;
        this.vendorService = vendorService;
        this.productFactory = productFactory;
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
    @Transactional(rollbackFor = IOException.class)
    public ProductResponse createProduct(CreateProductCommand command) throws IOException {

        VendorResponse vendor = command.getVendorId() != null && SecurityUtils.isAdmin()
                ? vendorService.getById(command.getVendorId())
                : vendorService.getByUser(SecurityUtils.currentUserId());
        if (vendor == null) {
            throw new ResourceNotFoundException("Vendor not found");
        }

        Map<String, List<Long>> variantAttributeValueMap = command.getVariants().stream()
                .collect(Collectors.toMap(
                        variant -> variant.getSku(),
                        variant -> variant.getAttributeValueIds() != null ? variant.getAttributeValueIds()
                                : List.of()));

        Product product = productFactory.create(command);

        Product persistedProduct = productRepository.save(product);
        Map<Long, ProductAttributeValue> persistedValuesByTempId = mapPersistedAttributeValues(command, persistedProduct);

        persistedProduct.getVariants().forEach(variant -> {
            variant.setProduct(persistedProduct);
            List<Long> attributeValueIds = variantAttributeValueMap.get(variant.getSku());
            Set<ProductAttributeValue> attributeValues = new HashSet<>();

            if (attributeValueIds != null) {
                attributeValueIds.forEach(tempId -> {
                    ProductAttributeValue value = persistedValuesByTempId.get(tempId);
                    if (value == null) {
                        throw new IllegalArgumentException("Unknown attribute value tempId: " + tempId);
                    }
                    attributeValues.add(value);
                });
            }

            // The setter rebuilds the key using the database-generated value IDs.
            variant.setAttributeValues(attributeValues);
        });

        Product savedProduct = productRepository.save(persistedProduct);

        return new ProductResponse(savedProduct).withUrl(baseUrl).withVendor(vendor);
    }

    private Map<Long, ProductAttributeValue> mapPersistedAttributeValues(CreateProductCommand command, Product product) {
        Map<Long, ProductAttributeValue> valuesByTempId = new HashMap<>();
        if (command.getAttributes() == null || command.getAttributes().isEmpty()) {
            return valuesByTempId;
        }

        Map<String, ProductAttribute> attributesByCode = product.getAttributes().stream()
                .collect(Collectors.toMap(ProductAttribute::getCode, attribute -> attribute));
        for (var attributeCommand : command.getAttributes()) {
            ProductAttribute attribute = attributesByCode.get(attributeCommand.getCode());
            if (attribute == null) {
                throw new IllegalStateException("Persisted attribute not found: " + attributeCommand.getCode());
            }
            Map<String, ProductAttributeValue> valuesByName = attribute.getValues().stream()
                    .collect(Collectors.toMap(ProductAttributeValue::getValue, value -> value));
            for (var valueCommand : attributeCommand.getValues()) {
                ProductAttributeValue value = valuesByName.get(valueCommand.getValue());
                if (value == null || value.getId() == null) {
                    throw new IllegalStateException("Persisted attribute value not found: " + valueCommand.getValue());
                }
                if (valuesByTempId.putIfAbsent(valueCommand.getTempId(), value) != null) {
                    throw new IllegalArgumentException("Duplicate attribute value tempId: " + valueCommand.getTempId());
                }
            }
        }
        return valuesByTempId;
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public ProductResponse updateProduct(UUID productId, UpdateProductCommand command) throws IOException {
        Product product = productRepository.findById(productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product not found");
        }

        product.setName(command.getName());
        product.setDescription(command.getDescription());
        product.setBrand(findBrand(command.getBrandId()));
        if (command.getStatus() != null) {
            product.setStatus(command.getStatus());
        }

        Set<Category> categories = findCategories(command.getCategoryIds());
        if (product.getCategories() == null) {
            product.setCategories(categories);
        } else {
            product.getCategories().clear();
            product.getCategories().addAll(categories);
        }
        if (command.getVariants() != null) {
            updateVariants(product, command.getVariants());
        }

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
    @Transactional(readOnly = true)
    public ProductVariantResponse getProductVariant(UUID productVariantId) {
        ProductVariant productVariant = productRepository.findVariantById(productVariantId);
        return productVariant != null ? new ProductVariantResponse(productVariant).withUrl(baseUrl) : null;
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

    private void updateVariants(Product product,
            List<UpdateProductCommand.UpdateProductVariantCommand> commands) throws IOException {
        if (product.getVariants() == null) {
            product.setVariants(new HashSet<>());
        }
        Map<UUID, ProductVariant> existingVariants = product.getVariants().stream()
                .filter(variant -> variant.getId() != null)
                .collect(Collectors.toMap(ProductVariant::getId, variant -> variant));
        Set<UUID> retainedIds = new HashSet<>();
        List<ProductVariant> newVariants = new ArrayList<>();

        for (var command : commands) {
            ProductVariant variant;
            if (command.getId() == null) {
                variant = ProductVariant.builder().product(product).images(new ArrayList<>()).build();
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

        // Keep existing entities and their attribute selections; remove only omitted
        // variants.
        product.getVariants().removeIf(variant -> !retainedIds.contains(variant.getId()));
        product.getVariants().addAll(newVariants);
    }

    private void updateImages(ProductVariant variant,
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
                variant.getImages().add(new File(null, url, variant));
            } else if (variant.getImages().stream().noneMatch(image -> command.getId().equals(image.getId()))) {
                throw new IllegalArgumentException("Image not found in variant: " + command.getId());
            }
        }
    }

}
