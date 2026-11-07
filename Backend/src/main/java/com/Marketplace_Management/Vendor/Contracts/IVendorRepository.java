package com.Marketplace_Management.Vendor.Contracts;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.Marketplace_Management.Vendor.DTOs.Command.GetListVendorCommand;
import com.Marketplace_Management.Vendor.DTOs.Response.VendorResponse;
import com.Marketplace_Management.Vendor.Entities.Vendor;

public interface IVendorRepository {
    Vendor save(Vendor vendor);

    List<VendorResponse> findAll(GetListVendorCommand command);

    Optional<Vendor> findById(UUID id);

    Optional<Vendor> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
