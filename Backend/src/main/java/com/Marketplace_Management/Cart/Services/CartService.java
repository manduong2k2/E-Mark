package com.Marketplace_Management.Cart.Services;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Marketplace_Management.Catalog.Contracts.IProductService;
import com.Marketplace_Management.Catalog.DTOs.Response.ProductVariantResponse;
import com.Marketplace_Management.Cart.Contracts.ICartRepository;
import com.Marketplace_Management.Cart.Contracts.ICartService;
import com.Marketplace_Management.Cart.DTOs.Commands.AddToCartCommand;
import com.Marketplace_Management.Cart.DTOs.Commands.CheckoutCartCommand;
import com.Marketplace_Management.Cart.DTOs.Commands.RemoveFromCartCommand;
import com.Marketplace_Management.Cart.DTOs.Commands.UpdateCartItemCommand;
import com.Marketplace_Management.Cart.DTOs.Responses.CartResponse;
import com.Marketplace_Management.Cart.DTOs.Responses.CartItemResponse;
import com.Marketplace_Management.Cart.Entities.Cart;
import com.Marketplace_Management.Cart.Entities.CartItem;
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
    @Transactional
    public CartResponse addItem(AddToCartCommand command) {
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
        return CartResponse.from(repository.update(cart));
    }

    @Override
    @Transactional
    public CartResponse removeItem(RemoveFromCartCommand command) {
        Cart cart = requireCart(command.getUserId());
        cart.removeItem(command.getProductVariantId());
        return CartResponse.from(repository.update(cart));
    }

    @Override
    @Transactional
    public CartResponse updateItem(UpdateCartItemCommand command) {
        Cart cart = requireCart(command.getUserId());
        cart.updateItemQuantity(command.getProductVariantId(), command.getQuantity());
        return withProductDetails(repository.update(cart));
    }

    @Override
    @Transactional
    public CartResponse checkout(CheckoutCartCommand command) {
        Cart cart = requireCart(command.getUserId());
        cart.checkout();
        return withProductDetails(repository.update(cart));
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getByUserId(UUID userId) {
        return repository.findByUserId(userId).map(this::withProductDetails).orElse(null);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        Cart cart = repository.findByUserId(userId).orElse(null);

        if(cart == null) {
            return;
        }

        cart.clear();
        repository.update(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getById(UUID id) {
        return repository.findById(id).map(CartResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
    }

    @Override
    @Transactional
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
    private CartResponse withProductDetails(Cart cart) {
        CartResponse response = CartResponse.from(cart);
        for (CartItemResponse item : response.getItems()) {
            ProductVariantResponse variant = productService.getProductVariant(item.getProductVariantId());
            if (variant == null) {
                continue; // variant deleted since it was added: keep the line, without details
            }
            item.setProductName(variant.getProduct().getName());
            item.setProductPrice(variant.getPrice());
            item.setProductImages(variant.getImages());
            item.setProductSku(variant.getSku());
            item.setSubTotal(item.getQuantity() * item.getProductPrice());
        }
        response.setTotal(response.getItems().stream().mapToDouble(CartItemResponse::getSubTotal).sum());
        return response;
    }
}
