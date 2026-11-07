package com.Marketplace_Management.Cart.Repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.Marketplace_Management.Cart.Contracts.ICartRepository;
import com.Marketplace_Management.Cart.Entities.CartEntity;

@Repository
public class CartRepository implements ICartRepository {

    private final JpaCartRepository jpa;

    public CartRepository(JpaCartRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<CartEntity> findAll() {
        return jpa.findAll();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CartEntity create(CartEntity cart) {
        // A concurrent first-cart insert must not mark the caller's transaction rollback-only.
        return jpa.saveAndFlush(cart);
    }

    @Override
    public Optional<CartEntity> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<CartEntity> findByUserId(UUID userId) {
        return jpa.findByUserId(userId);
    }

    @Override
    public CartEntity update(CartEntity cart) {
        return jpa.save(cart);
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public List<CartEntity> findByItemsProductVariantId(UUID productVariantId) {
        return jpa.findByItemsProductVariantId(productVariantId);
    }
}
