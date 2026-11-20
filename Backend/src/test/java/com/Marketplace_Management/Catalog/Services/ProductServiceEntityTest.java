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
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.Marketplace_Management.Catalog.Contracts.IProductFactory;
import com.Marketplace_Management.Catalog.Contracts.IProductRepository;
import com.Marketplace_Management.Catalog.DTOs.Commands.Product.CreateProductCommand;
import com.Marketplace_Management.Catalog.DTOs.Commands.Product.UpdateProductCommand;
import com.Marketplace_Management.Catalog.Entities.Brand;
import com.Marketplace_Management.Catalog.Entities.ProductAttribute;
import com.Marketplace_Management.Catalog.Entities.ProductAttributeValue;
import com.Marketplace_Management.Catalog.Entities.Product;
import com.Marketplace_Management.Catalog.Entities.ProductVariant;
import com.Marketplace_Management.Shared.Contracts.IEventPublisher;
import com.Marketplace_Management.Shared.Contracts.IFileService;
import com.Marketplace_Management.Shared.Entities.File;
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
    @Mock IProductFactory productFactory;
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
        File retainedImage = new File(UUID.randomUUID(), "uploads/retained.png", retained);
        File removedImage = new File(UUID.randomUUID(), "uploads/removed.png", retained);
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
    void createPersistsFactoryProductAndReturnsVendor() throws Exception {
        UUID userId = UUID.randomUUID();
        VendorResponse vendor = new VendorResponse();
        vendor.setId(UUID.randomUUID());
        var upload = new MockMultipartFile("image", "new.png", "image/png", new byte[] {1});
        Product factoryProduct = product();
        factoryProduct.setId(null);
        factoryProduct.setVendorId(vendor.getId());
        factoryProduct.setAttributes(new HashSet<>());
        ProductVariant first = variant(factoryProduct, "SKU-1");
        first.setId(null);
        first.getImages().add(new File(null, "uploads/new.png", first));
        ProductVariant second = variant(factoryProduct, "SKU-2");
        second.setId(null);

        when(vendorService.getByUser(userId)).thenReturn(vendor);
        when(productRepository.save(any())).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            assertSame(factoryProduct, product);
            assertEquals(2, product.getVariants().size());
            for (ProductVariant variant : product.getVariants()) {
                assertSame(product, variant.getProduct());
                variant.getImages().forEach(image -> assertSame(variant, image.getEntity()));
                if (variant.getId() == null) {
                    variant.setId(UUID.randomUUID());
                }
                assertTrue(product.getVariants().contains(variant));
            }
            if (product.getId() == null) {
                product.setId(UUID.randomUUID());
            }
            return product;
        });
        var command = CreateProductCommand.builder().name("Product").brandId(factoryProduct.getBrand().getId())
                .variants(List.of(
                        CreateProductCommand.CreateProductVariantCommand.builder()
                                .name("Variant").sku("SKU-1").price(10).stock(1).images(List.of(upload)).build(),
                        CreateProductCommand.CreateProductVariantCommand.builder()
                                .name("Variant").sku("SKU-2").price(10).stock(1).build()))
                .build();
        when(productFactory.create(command)).thenReturn(factoryProduct);

        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::currentUserId).thenReturn(userId);
            var response = service.createProduct(command);
            assertEquals(factoryProduct.getId(), response.getId());
            assertEquals(2, response.getVariants().size());
            assertEquals(vendor.getId(), response.getVendorId());
            assertSame(vendor, response.getVendor());
            assertEquals("", first.getAttributeValuesKey());
            assertEquals("", second.getAttributeValuesKey());
            verify(productFactory).create(command);
            verifyNoInteractions(brandRepository, fileService);
        }
    }

    @Test
    void createRebuildsVariantKeysFromPersistedValueIdsInsteadOfTempIds() throws Exception {
        UUID userId = UUID.randomUUID();
        VendorResponse vendor = new VendorResponse();
        vendor.setId(UUID.randomUUID());
        Product product = product();
        product.setVendorId(vendor.getId());
        ProductAttribute color = ProductAttribute.builder().code("color").product(product).build();
        ProductAttribute size = ProductAttribute.builder().code("size").product(product).build();
        ProductAttributeValue red = ProductAttributeValue.builder().value("Red").productAttribute(color).build();
        ProductAttributeValue blue = ProductAttributeValue.builder().value("Blue").productAttribute(color).build();
        ProductAttributeValue small = ProductAttributeValue.builder().value("S").productAttribute(size).build();
        color.getValues().addAll(List.of(red, blue));
        size.getValues().add(small);
        product.setAttributes(new HashSet<>(List.of(color, size)));
        ProductVariant redVariant = variant(product, "RED-S");
        ProductVariant blueVariant = variant(product, "BLUE-S");
        redVariant.setAttributeValues(new HashSet<>(List.of(red, small)));
        blueVariant.setAttributeValues(new HashSet<>(List.of(blue, small)));

        var command = CreateProductCommand.builder().name("Product").brandId(product.getBrand().getId())
                .attributes(List.of(
                        CreateProductCommand.CreateProductAttributeCommand.builder().code("color")
                                .values(List.of(
                                        CreateProductCommand.CreateProductAttributeValueCommand.builder()
                                                .tempId(3001L).value("Red").build(),
                                        CreateProductCommand.CreateProductAttributeValueCommand.builder()
                                                .tempId(42L).value("Blue").build()))
                                .build(),
                        CreateProductCommand.CreateProductAttributeCommand.builder().code("size")
                                .values(List.of(CreateProductCommand.CreateProductAttributeValueCommand.builder()
                                        .tempId(5001L).value("S").build()))
                                .build()))
                .variants(List.of(
                        CreateProductCommand.CreateProductVariantCommand.builder().sku("RED-S")
                                .attributeValueIds(List.of(5001L, 3001L)).build(),
                        CreateProductCommand.CreateProductVariantCommand.builder().sku("BLUE-S")
                                .attributeValueIds(List.of(42L, 5001L)).build()))
                .build();
        when(vendorService.getByUser(userId)).thenReturn(vendor);
        when(productFactory.create(command)).thenReturn(product);
        when(productRepository.save(product)).thenAnswer(invocation -> {
            // Simulate generated IDs becoming available during persistence.
            red.setId(42L);
            blue.setId(70L);
            small.setId(9L);
            return product;
        });

        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::currentUserId).thenReturn(userId);
            service.createProduct(command);
        }

        assertEquals(new HashSet<>(List.of(red, small)), redVariant.getAttributeValues());
        assertEquals(new HashSet<>(List.of(blue, small)), blueVariant.getAttributeValues());
        assertEquals("42,9", redVariant.getAttributeValuesKey());
        assertEquals("70,9", blueVariant.getAttributeValuesKey());
    }

    @Test
    void createRejectsUnknownAttributeValueTempId() throws Exception {
        UUID userId = UUID.randomUUID();
        VendorResponse vendor = new VendorResponse();
        vendor.setId(UUID.randomUUID());
        Product product = product();
        product.setAttributes(new HashSet<>());
        variant(product, "SKU-1");
        var command = CreateProductCommand.builder()
                .variants(List.of(CreateProductCommand.CreateProductVariantCommand.builder().sku("SKU-1")
                        .attributeValueIds(List.of(999L)).build()))
                .build();
        when(vendorService.getByUser(userId)).thenReturn(vendor);
        when(productFactory.create(command)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);

        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::currentUserId).thenReturn(userId);
            var exception = assertThrows(IllegalArgumentException.class, () -> service.createProduct(command));
            assertEquals("Unknown attribute value tempId: 999", exception.getMessage());
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
