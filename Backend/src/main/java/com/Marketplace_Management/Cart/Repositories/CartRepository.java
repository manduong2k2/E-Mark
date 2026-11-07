package com.Marketplace_Management.Cart.Repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.Marketplace_Management.Cart.Contracts.ICartRepository;
import com.Marketplace_Management.Cart.Entities.Cart;

@Repository
public class CartRepository implements ICartRepository {

    private final JpaCartRepository jpa;

    public CartRepository(JpaCartRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Cart> findAll() {
        return jpa.findAll();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Cart create(Cart cart) {
        // A concurrent first-cart insert must not mark the caller's transaction rollback-only.
        return jpa.saveAndFlush(cart);
    }

    @Override
    public Optional<Cart> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Cart> findByUserId(UUID userId) {
        return jpa.findByUserId(userId);
    }

    @Override
    public Cart update(Cart cart) {
        return jpa.save(cart);
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public List<Cart> findByItemsProductVariantId(UUID productVariantId) {
        return jpa.findByItemsProductVariantId(productVariantId);
    }
}
