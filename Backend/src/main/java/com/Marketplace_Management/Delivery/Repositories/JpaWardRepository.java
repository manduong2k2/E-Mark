package com.Marketplace_Management.Delivery.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.Marketplace_Management.Delivery.Entities.Ward;

public interface JpaWardRepository extends JpaRepository<Ward, String> {
    @Override
    @EntityGraph(attributePaths = "province")
    Optional<Ward> findById(String wardId);

    List<Ward> findByProvinceId(String provinceId);
}
