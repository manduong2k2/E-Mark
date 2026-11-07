package com.Marketplace_Management.Catalog.Repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Marketplace_Management.Catalog.Entities.Product;

public interface ProductJpaRepository extends JpaRepository<Product, UUID> {
    List<Product> findByName(String name);    
}
