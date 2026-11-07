package com.Marketplace_Management.Cart.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.Marketplace_Management.Cart.Constants.CartStatusEnum;
import com.Marketplace_Management.Cart.Contracts.ICartRepository;
import com.Marketplace_Management.Cart.DTOs.Commands.AddToCartCommand;
import com.Marketplace_Management.Cart.Entities.CartEntity;
import com.Marketplace_Management.Cart.Entities.CartItemEntity;
import com.Marketplace_Management.Catalog.Contracts.IProductService;
import com.Marketplace_Management.Catalog.DTOs.Response.ProductResponse;
import com.Marketplace_Management.Catalog.DTOs.Response.ProductVariantResponse;

@ExtendWith(MockitoExtension.class)
class CartServiceEntityTest {
    @Mock ICartRepository repository;
    @Mock IProductService productService;
    @InjectMocks CartService service;

    @Test
    void cartMergesQuantitiesAndRemovesItemsWithoutReplacingItsCollection() {
        CartEntity cart = CartEntity.builder().userId(UUID.randomUUID()).build();
        var items = cart.getItems();
        UUID variantId = UUID.randomUUID();
        CartItemEntity item = CartItemEntity.builder().productVariantId(variantId).quantity(2).build();
        cart.addItem(item);
        cart.addItem(CartItemEntity.builder().productVariantId(variantId).quantity(3).build());

        assertEquals(CartStatusEnum.NEW.name(), cart.getStatus());
        assertSame(items, cart.getItems());
        assertEquals(1, items.size());
        assertSame(cart, item.getCart());
        assertEquals(5, item.getQuantity());

        cart.updateItemQuantity(variantId, 0);
        assertSame(items, cart.getItems());
        assertTrue(items.isEmpty());
    }

    @Test
    void cartKeepsCheckoutAndMutationRulesOnTheEntity() {
        CartEntity cart = CartEntity.builder().build();
        assertThrows(IllegalStateException.class, cart::checkout);
        CartItemEntity item = CartItemEntity.builder().productVariantId(UUID.randomUUID()).quantity(1).build();
        cart.addItem(item);
        cart.checkout();

        assertEquals(CartStatusEnum.CHECKED_OUT.name(), cart.getStatus());
        assertThrows(IllegalStateException.class, cart::clear);
        assertThrows(IllegalStateException.class, () -> cart.updateItemQuantity(item.getProductVariantId(), 2));
        assertThrows(IllegalStateException.class, () -> cart.removeItem(item.getProductVariantId()));
        assertThrows(IllegalStateException.class, () -> cart.addItem(item));
    }

    @Test
    void readsEnrichDtosAndKeepDeletedVariantsWithoutWritingTheCart() {
        CartEntity cart = CartEntity.builder().id(UUID.randomUUID()).userId(UUID.randomUUID()).build();
        UUID variantId = UUID.randomUUID();
        UUID deletedVariantId = UUID.randomUUID();
        cart.addItem(CartItemEntity.builder().productVariantId(variantId).quantity(2).build());
        cart.addItem(CartItemEntity.builder().productVariantId(deletedVariantId).quantity(1).build());
        ProductResponse product = new ProductResponse();
        product.setName("Product");
        ProductVariantResponse variant = new ProductVariantResponse();
        variant.setProduct(product);
        variant.setPrice(12.5);
        variant.setSku("SKU-1");
        variant.setImages(List.of("http://api/image.png"));
        when(repository.findByUserId(cart.getUserId())).thenReturn(Optional.of(cart));
        when(productService.getProductVariant(variantId)).thenReturn(variant);
        when(productService.getProductVariant(deletedVariantId)).thenReturn(null);

        var response = service.getByUserId(cart.getUserId());

        assertEquals(2, response.getTotalItemCount());
        assertEquals(25, response.getTotal());
        assertEquals("Product", response.getItems().get(0).getProductName());
        assertEquals("SKU-1", response.getItems().get(0).getProductSku());
        assertEquals(25, response.getItems().get(0).getSubTotal());
        assertEquals(0, response.getItems().get(1).getSubTotal());
        assertEquals(2, cart.getItems().size());
        verify(repository).findByUserId(cart.getUserId());
    }

    @Test
    void concurrentFirstCartCreationFallsBackToTheExistingCart() {
        UUID userId = UUID.randomUUID();
        CartEntity cart = CartEntity.builder().id(UUID.randomUUID()).userId(userId).build();
        UUID variantId = UUID.randomUUID();
        CartItemEntity item = CartItemEntity.builder().id(UUID.randomUUID()).productVariantId(variantId).quantity(1).build();
        cart.addItem(item);
        when(repository.findByUserId(userId)).thenReturn(Optional.empty(), Optional.of(cart));
        when(repository.create(any())).thenThrow(new DataIntegrityViolationException("Concurrent cart insert"));
        when(repository.update(cart)).thenReturn(cart);

        var response = service.addItem(new AddToCartCommand(userId, variantId, 2));

        assertEquals(cart.getId(), response.getId());
        assertEquals(1, cart.getItems().size());
        assertSame(item, cart.getItems().get(0));
        assertEquals(3, item.getQuantity());
        assertSame(cart, item.getCart());
        verifyNoInteractions(productService);
    }

    @Test
    void clearUsesTheExistingCollectionWithoutFetchingProductDetails() {
        CartEntity cart = CartEntity.builder().userId(UUID.randomUUID()).build();
        cart.addItem(CartItemEntity.builder().productVariantId(UUID.randomUUID()).quantity(1).build());
        var items = cart.getItems();
        when(repository.findByUserId(cart.getUserId())).thenReturn(Optional.of(cart));

        service.clearCart(cart.getUserId());

        assertSame(items, cart.getItems());
        assertTrue(items.isEmpty());
        verify(repository).update(cart);
        verifyNoInteractions(productService);
    }
}
