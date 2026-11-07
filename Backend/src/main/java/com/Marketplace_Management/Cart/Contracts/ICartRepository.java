package com.Marketplace_Management.Cart.Contracts;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.Marketplace_Management.Cart.Entities.CartEntity;

public interface ICartRepository {
    List<CartEntity> findAll();
    CartEntity create(CartEntity cart);
    Optional<CartEntity> findById(UUID id);
    Optional<CartEntity> findByUserId(UUID userId);
    CartEntity update(CartEntity cart);
    void delete(UUID id);
    List<CartEntity> findByItemsProductVariantId(UUID productVariantId);
}
