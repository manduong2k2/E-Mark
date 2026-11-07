package com.Marketplace_Management.Delivery.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Marketplace_Management.Delivery.Entities.Province;

public interface JpaProvinceRepository extends JpaRepository<Province, String> {
    
}
