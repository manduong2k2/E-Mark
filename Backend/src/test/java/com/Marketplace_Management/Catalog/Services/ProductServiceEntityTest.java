package com.Marketplace_Management.Catalog.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.Marketplace_Management.Catalog.Contracts.IBrandRepository;
import com.Marketplace_Management.Catalog.Contracts.ICategoryRepository;
import com.Marketplace_Management.Catalog.Contracts.IProductRepository;
import com.Marketplace_Management.Catalog.DTOs.Commands.Product.CreateProductCommand;
import com.Marketplace_Management.Catalog.DTOs.Commands.Product.UpdateProductCommand;
import com.Marketplace_Management.Catalog.Entities.Brand;
import com.Marketplace_Management.Catalog.Entities.ProductAttributeValue;
import com.Marketplace_Management.Catalog.Entities.Product;
import com.Marketplace_Management.Catalog.Entities.ProductVariant;
import com.Marketplace_Management.Shared.Contracts.IEventPublisher;
import com.Marketplace_Management.Shared.Contracts.IFileService;
import com.Marketplace_Management.Shared.Entities.FileEntity;
import com.Marketplace_Management.Shared.Security.SecurityUtils;
import com.Marketplace_Management.Vendor.Contracts.IVendorService;
import com.Marketplace_Management.Vendor.DTOs.Response.VendorResponse;

@ExtendWith(MockitoExtension.class)
class ProductServiceEntityTest {
    @Mock IProductRepository productRepository;
    @Mock IBrandRepository brandRepository;
    @Mock ICategoryRepository categoryRepository;
    @Mock IEventPublisher eventPublisher;
    @Mock IFileService fileService;
    @Mock IVendorService vendorService;
    @InjectMocks ProductService service;

    @BeforeEach
    void setBaseUrl() {
        ReflectionTestUtils.setField(service, "baseUrl", "http://api");
    }

    @Test
    void updatePreservesEntityCollectionsAndAttributeSelections() throws Exception {
        Product product = product();
        ProductVariant retained = variant(product, "SKU-1");
        ProductVariant removed = variant(product, "SKU-2");
        ProductAttributeValue value = ProductAttributeValue.builder().id(1L).value("Red").build();
        retained.getAttributeValues().add(value);
        FileEntity retainedImage = new FileEntity(UUID.randomUUID(), "uploads/retained.png", retained);
        FileEntity removedImage = new FileEntity(UUID.randomUUID(), "uploads/removed.png", retained);
        retained.getImages().addAll(List.of(retainedImage, removedImage));
        var variants = product.getVariants();
        var images = retained.getImages();
        var values = retained.getAttributeValues();
        var upload = new MockMultipartFile("image", "new.png", "image/png", new byte[] {1});

        when(productRepository.findById(product.getId())).thenReturn(product);
        when(brandRepository.findById(product.getBrand().getId())).thenReturn(Optional.of(product.getBrand()));
        when(productRepository.update(product)).thenReturn(product);
        when(fileService.uploadFile(upload, "catalog/product_variants")).thenReturn("uploads/new.png");

        service.updateProduct(product.getId(), UpdateProductCommand.builder()
                .name("Updated product").brandId(product.getBrand().getId())
                .variants(List.of(
                        UpdateProductCommand.UpdateProductVariantCommand.builder()
                                .id(retained.getId()).name("Updated variant").sku("SKU-1").price(25).stock(7)
                                .images(List.of(
                                        UpdateProductCommand.UpdateFileCommand.builder().id(retainedImage.getId()).build(),
                                        UpdateProductCommand.UpdateFileCommand.builder().file(upload).build()))
                                .build(),
                        UpdateProductCommand.UpdateProductVariantCommand.builder()
                                .name("New variant").sku("SKU-3").price(30).stock(4).build()))
                .build());

        assertSame(variants, product.getVariants());
        assertTrue(variants.contains(retained));
        assertFalse(variants.contains(removed));
        assertEquals(2, variants.size());
        assertEquals(25, retained.getPrice());
        assertEquals("PUBLISHED", product.getStatus());
        assertSame(values, retained.getAttributeValues());
        assertSame(value, values.iterator().next());
        assertSame(images, retained.getImages());
        assertSame(retainedImage, images.get(0));
        assertEquals(2, images.size());
        assertEquals("uploads/new.png", images.get(1).getUrl());
        assertSame(retained, images.get(1).getEntity());
        ProductVariant added = variants.stream().filter(v -> "SKU-3".equals(v.getSku())).findFirst().orElseThrow();
        assertSame(product, added.getProduct());
        assertTrue(added.getAttributeValues().isEmpty());
    }

    @Test
    void updateRejectsVariantsFromAnotherProduct() {
        Product product = product();
        variant(product, "SKU-1");
        when(productRepository.findById(product.getId())).thenReturn(product);
        when(brandRepository.findById(product.getBrand().getId())).thenReturn(Optional.of(product.getBrand()));

        var command = UpdateProductCommand.builder().name("Updated").brandId(product.getBrand().getId())
                .variants(List.of(UpdateProductCommand.UpdateProductVariantCommand.builder()
                        .id(UUID.randomUUID()).name("Foreign variant").sku("OTHER").build()))
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.updateProduct(product.getId(), command));
        verify(productRepository, never()).update(any());
    }

    @Test
    void createConnectsVariantAndImageOwnersBeforeSavingWithoutCollapsingNewVariants() throws Exception {
        UUID userId = UUID.randomUUID();
        Brand brand = Brand.builder().id(UUID.randomUUID()).name("Brand").build();
        VendorResponse vendor = new VendorResponse();
        vendor.setId(UUID.randomUUID());
        var upload = new MockMultipartFile("image", "new.png", "image/png", new byte[] {1});
        when(vendorService.getByUser(userId)).thenReturn(vendor);
        when(brandRepository.findById(brand.getId())).thenReturn(Optional.of(brand));
        when(fileService.uploadFile(upload, "catalog/product_variants")).thenReturn("uploads/new.png");
        when(productRepository.save(any())).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            assertEquals(2, product.getVariants().size());
            for (ProductVariant variant : product.getVariants()) {
                assertSame(product, variant.getProduct());
                variant.getImages().forEach(image -> assertSame(variant, image.getEntity()));
                variant.setId(UUID.randomUUID());
                assertTrue(product.getVariants().contains(variant));
            }
            product.setId(UUID.randomUUID());
            return product;
        });
        var command = CreateProductCommand.builder().name("Product").brandId(brand.getId())
                .variants(List.of(
                        CreateProductCommand.CreateProductVariantCommand.builder()
                                .name("Variant").sku("SKU-1").price(10).stock(1).images(List.of(upload)).build(),
                        CreateProductCommand.CreateProductVariantCommand.builder()
                                .name("Variant").sku("SKU-2").price(10).stock(1).build()))
                .build();

        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::currentUserId).thenReturn(userId);
            var response = service.createProduct(command);
            assertEquals(2, response.getVariants().size());
            assertEquals(vendor.getId(), response.getVendorId());
            assertSame(vendor, response.getVendor());
        }
    }

    private Product product() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Product");
        product.setStatus("PUBLISHED");
        product.setBrand(Brand.builder().id(UUID.randomUUID()).name("Brand").build());
        product.setCategories(new HashSet<>());
        product.setVariants(new HashSet<>());
        return product;
    }

    private ProductVariant variant(Product product, String sku) {
        ProductVariant variant = ProductVariant.builder().id(UUID.randomUUID())
                .name("Variant").sku(sku).price(10).stock(1).product(product).images(new ArrayList<>()).build();
        product.getVariants().add(variant);
        return variant;
    }
}
