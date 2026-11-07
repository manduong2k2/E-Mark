package com.Marketplace_Management.Catalog.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Marketplace_Management.Catalog.Entities.ProductVariant;

import java.util.Optional;
import java.util.UUID;

public interface ProductVariantJpaRepository extends JpaRepository<ProductVariant, UUID> {
    Optional<ProductVariant> findById(UUID id);
}
