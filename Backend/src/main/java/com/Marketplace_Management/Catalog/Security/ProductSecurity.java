package com.Marketplace_Management.Catalog.Security;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.Marketplace_Management.Catalog.Contracts.IProductRepository;
import com.Marketplace_Management.Catalog.Models.Product;
import com.Marketplace_Management.Shared.Security.SecurityUtils;
import com.Marketplace_Management.Vendor.Contracts.IVendorService;

/** Used in @PreAuthorize("@productSecurity.canManage(#productId)"): an admin, or the owner of the product's vendor. */
@Component
public class ProductSecurity {
    private final IProductRepository productRepository;
    private final IVendorService vendorService;

    public ProductSecurity(IProductRepository productRepository, IVendorService vendorService) {
        this.productRepository = productRepository;
        this.vendorService = vendorService;
    }

    public boolean canManage(UUID productId) {
        if (SecurityUtils.isAdmin()) {
            return true;
        }
        Product product = productRepository.findById(productId);
        if (product == null || product.getVendorId() == null) {
            return false;
        }
        return SecurityUtils.isOwnerOrAdmin(vendorService.getById(product.getVendorId()).getUserId());
    }
}
