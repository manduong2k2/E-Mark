package com.Marketplace_Management.Vendor.Repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Marketplace_Management.Vendor.Entities.Collection;

import java.util.Set;

public interface CollectionJpaRepository extends JpaRepository<Collection, UUID> {
    Set<Collection> findByVendorId(UUID vendorId);
}
