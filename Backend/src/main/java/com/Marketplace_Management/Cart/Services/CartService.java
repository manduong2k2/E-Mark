package com.Marketplace_Management.Cart.Services;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.Marketplace_Management.Catalog.Contracts.IProductService;
import com.Marketplace_Management.Catalog.DTOs.Response.ProductVariantResponse;
import com.Marketplace_Management.Cart.Contracts.ICartRepository;
import com.Marketplace_Management.Cart.Contracts.ICartService;
import com.Marketplace_Management.Cart.DTOs.Commands.AddToCartCommand;
import com.Marketplace_Management.Cart.DTOs.Commands.CheckoutCartCommand;
import com.Marketplace_Management.Cart.DTOs.Commands.RemoveFromCartCommand;
import com.Marketplace_Management.Cart.DTOs.Commands.UpdateCartItemCommand;
import com.Marketplace_Management.Cart.Models.Cart.Cart;
import com.Marketplace_Management.Cart.Models.Cart.CartItem;
import com.Marketplace_Management.Shared.Errors.Exceptions.ResourceNotFoundException;

@Service
public class CartService implements ICartService {

    private final ICartRepository repository;
    private final IProductService productService;

    public CartService(ICartRepository repository, IProductService productService) {
        this.repository = repository;
        this.productService = productService;
    }

    @Override
    public Cart addItem(AddToCartCommand command) {
        Cart cart = repository.findByUserId(command.getUserId())
                .orElseGet(() -> {
                    try {
                        return repository.create(Cart.builder().userId(command.getUserId()).build());
                    } catch (DataIntegrityViolationException e) {
                        return repository.findByUserId(command.getUserId()).orElseThrow();
                    }
                });

        cart.addItem(CartItem.builder()
                .productVariantId(command.getProductVariantId())
                .quantity(command.getQuantity())
                .build());
        Cart updated = repository.update(cart);

        return updated;
    }

    @Override
    public Cart removeItem(RemoveFromCartCommand command) {
        Cart cart = requireCart(command.getUserId());
        cart.removeItem(command.getProductVariantId());
        Cart updated = repository.update(cart);
        return updated;
    }

    @Override
    public Cart updateItem(UpdateCartItemCommand command) {
        Cart cart = requireCart(command.getUserId());
        cart.updateItemQuantity(command.getProductVariantId(), command.getQuantity());
        return withProductDetails(repository.update(cart));
    }

    @Override
    public Cart checkout(CheckoutCartCommand command) {
        Cart cart = requireCart(command.getUserId());
        cart.checkout();
        return withProductDetails(repository.update(cart));
    }

    @Override
    public Cart getByUserId(UUID userId) {
        return repository.findByUserId(userId).map(this::withProductDetails).orElse(null);
    }

    @Override
    public void clearCart(UUID userId) {
        Cart cart = getByUserId(userId);

        if(cart == null) {
            return;
        }

        cart.clear();
        repository.update(cart);
    }

    @Override
    public Cart getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
    }

    @Override
    public void removeItemsByProductVariantId(UUID productVariantId) {
        repository.findByItemsProductVariantId(productVariantId).forEach(cart -> {
            if (cart.getItems().stream().anyMatch(item -> item.getProductVariantId().equals(productVariantId))) {
                cart.removeItem(productVariantId);
                repository.update(cart);
            }
        });
    }

    private Cart requireCart(UUID userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
    }

    /** Fills each item with the current name / price / images of its product variant. */
    private Cart withProductDetails(Cart cart) {
        for (CartItem item : cart.getItems()) {
            ProductVariantResponse variant = productService.getProductVariant(item.getProductVariantId());
            if (variant == null) {
                continue; // variant deleted since it was added: keep the line, without details
            }
            item.setProductName(variant.getProduct().getName());
            item.setProductPrice(variant.getPrice());
            item.setProductImage(variant.getImages());
            item.setProductSku(variant.getSku());
        }
        return cart;
    }
}
