package com.Marketplace_Management.Cart.Repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Marketplace_Management.Cart.Entities.Cart;

public interface JpaCartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByUserId(UUID userId);
    List<Cart> findByItemsProductVariantId(UUID productId);
}
