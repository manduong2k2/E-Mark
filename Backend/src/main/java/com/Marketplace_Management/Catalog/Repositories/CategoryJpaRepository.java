package com.Marketplace_Management.Catalog.Repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Marketplace_Management.Catalog.Entities.Category;

public interface CategoryJpaRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByName(String name);
}
