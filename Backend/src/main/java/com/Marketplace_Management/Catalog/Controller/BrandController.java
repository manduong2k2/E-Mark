package com.Marketplace_Management.Catalog.Controller;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Marketplace_Management.Catalog.Contracts.IBrandService;
import com.Marketplace_Management.Catalog.DTOs.Commands.Brand.CreateBrandCommand;
import com.Marketplace_Management.Catalog.DTOs.Commands.Brand.GetListBrandCommand;
import com.Marketplace_Management.Catalog.DTOs.Commands.Brand.UpdateBrandCommand;
import com.Marketplace_Management.Catalog.DTOs.Requests.Brand.CreateBrandRequest;
import com.Marketplace_Management.Catalog.DTOs.Requests.Brand.GetListBrandRequest;
import com.Marketplace_Management.Catalog.DTOs.Requests.Brand.UpdateBrandRequest;
import com.Marketplace_Management.Shared.Constants.UserRole;
import com.Marketplace_Management.Shared.Controllers.BaseController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/brands")
public class BrandController extends BaseController {
    private final IBrandService brandService;

    public BrandController(IBrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll(@Valid @ModelAttribute GetListBrandRequest request) {
        return paginatedResponse(brandService.getAll(GetListBrandCommand.fromRequest(request)));
    }

    @PreAuthorize("hasAuthority('" + UserRole.ADMIN + "')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @ModelAttribute CreateBrandRequest request) throws IOException {
        return createdResponse(brandService.createBrand(CreateBrandCommand.fromRequest(request)));
    }

    @GetMapping("/{brandId}")
    public ResponseEntity<Map<String, Object>> details(@PathVariable UUID brandId) {
        return objectResponse(brandService.getBrand(brandId));
    }

    @PreAuthorize("hasAuthority('" + UserRole.ADMIN + "')")
    @PutMapping("/{brandId}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable UUID brandId,
            @Valid @ModelAttribute UpdateBrandRequest request) throws IOException {
        return objectResponse(brandService.updateBrand(brandId, UpdateBrandCommand.fromRequest(request)));
    }

    @PreAuthorize("hasAuthority('" + UserRole.ADMIN + "')")
    @DeleteMapping("/{brandId}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable UUID brandId) {
        brandService.deleteBrand(brandId);
        return successResponse("Brand deleted successfully");
    }
}
