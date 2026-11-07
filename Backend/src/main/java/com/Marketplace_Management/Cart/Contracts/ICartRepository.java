package com.Marketplace_Management.Cart.Contracts;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.Marketplace_Management.Cart.Entities.Cart;

public interface ICartRepository {
    List<Cart> findAll();
    Cart create(Cart cart);
    Optional<Cart> findById(UUID id);
    Optional<Cart> findByUserId(UUID userId);
    Cart update(Cart cart);
    void delete(UUID id);
    List<Cart> findByItemsProductVariantId(UUID productVariantId);
}
