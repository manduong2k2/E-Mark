package com.Marketplace_Management.Vendor.Contracts;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.Marketplace_Management.Vendor.DTOs.Command.GetListVendorCommand;
import com.Marketplace_Management.Vendor.DTOs.Response.VendorResponse;
import com.Marketplace_Management.Vendor.Entities.VendorEntity;

public interface IVendorRepository {
    VendorEntity save(VendorEntity vendor);

    List<VendorResponse> findAll(GetListVendorCommand command);

    Optional<VendorEntity> findById(UUID id);

    Optional<VendorEntity> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
