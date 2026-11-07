package com.Marketplace_Management.Catalog.DTOs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.Marketplace_Management.Catalog.DTOs.Response.ProductResponse;
import com.Marketplace_Management.Catalog.DTOs.Response.ProductVariantResponse;
import com.Marketplace_Management.Catalog.Entities.Brand;
import com.Marketplace_Management.Catalog.Entities.Category;
import com.Marketplace_Management.Catalog.Entities.Product;
import com.Marketplace_Management.Catalog.Entities.ProductVariant;
import com.Marketplace_Management.Shared.Entities.File;

import tools.jackson.databind.json.JsonMapper;

class CatalogResponseTest {
    @Test
    void responsesSerializeDtosWithoutTraversingEntityBackReferences() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Product");
        product.setBrand(Brand.builder().id(UUID.randomUUID()).name("Brand").image("uploads/brand.png").build());
        product.setCategories(new HashSet<>());
        Category parent = Category.builder().id(UUID.randomUUID()).name("Parent").build();
        Category child = Category.builder().id(UUID.randomUUID()).name("Child").parent(parent).build();
        parent.getChildren().add(child);
        product.getCategories().add(parent);
        product.setVariants(new HashSet<>());
        ProductVariant variant = ProductVariant.builder().id(UUID.randomUUID()).name("Variant")
                .price(12.5).stock(3).product(product).images(new ArrayList<>()).build();
        variant.getImages().add(new File(UUID.randomUUID(), "uploads/variant.png", variant));
        product.getVariants().add(variant);
        JsonMapper mapper = JsonMapper.builder().build();

        var detail = mapper.valueToTree(new ProductResponse(product).withUrl("http://api"));
        assertEquals(12.5, detail.path("variants").path(0).path("price").asDouble());
        assertEquals("http://api/uploads/variant.png", detail.path("variants").path(0).path("images").path(0).path("url").asString());
        assertEquals(parent.getId().toString(), detail.path("categories").path(0).path("children").path(0).path("parentId").asString());
        assertFalse(detail.path("variants").path(0).has("product"));
        assertFalse(detail.path("variants").path(0).path("images").path(0).has("entity"));

        var variantDetail = mapper.valueToTree(new ProductVariantResponse(variant).withUrl("http://api"));
        assertEquals("http://api/uploads/variant.png", variantDetail.path("images").path(0).asString());
        assertEquals(product.getId().toString(), variantDetail.path("product").path("id").asString());
        assertTrue(variantDetail.path("product").path("variants").isNull());
    }
}
