package com.Marketplace_Management.Catalog.Controller;

import java.io.IOException;
import java.util.List;
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

import com.Marketplace_Management.Catalog.Contracts.IProductService;
import com.Marketplace_Management.Catalog.DTOs.Commands.Product.CreateProductCommand;
import com.Marketplace_Management.Catalog.DTOs.Commands.Product.GetListProductCommand;
import com.Marketplace_Management.Catalog.DTOs.Commands.Product.UpdateProductCommand;
import com.Marketplace_Management.Catalog.DTOs.Requests.Product.CreateProductRequest;
import com.Marketplace_Management.Catalog.DTOs.Requests.Product.GetListProductRequest;
import com.Marketplace_Management.Catalog.DTOs.Requests.Product.UpdateProductRequest;
import com.Marketplace_Management.Catalog.DTOs.Response.ProductResponse;
import com.Marketplace_Management.Catalog.DTOs.Response.ProductShortResponse;
import com.Marketplace_Management.Catalog.DTOs.Response.ProductVariantResponse;
import com.Marketplace_Management.Shared.Constants.UserRole;
import com.Marketplace_Management.Shared.Controllers.BaseController;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController extends BaseController{
    private final IProductService productService;

    public ProductController(IProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<Map<String,Object>> getAll(@Valid @ModelAttribute GetListProductRequest request) {
        GetListProductCommand command = GetListProductCommand.fromRequest(request);
        PaginatedResponse<ProductShortResponse> products = productService.getAll(command);

        return paginatedResponse(products);
    }

    @GetMapping("/statuses")
    public ResponseEntity<Map<String,Object>> getAllStatus() {
        List<String> statuses = productService.getAllStatus();

        return objectResponse(statuses);
    }

    @PreAuthorize("hasAnyAuthority('" + UserRole.ADMIN + "', '" + UserRole.VENDOR + "')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @ModelAttribute CreateProductRequest request) throws IOException {
        CreateProductCommand command = request.validated();

        ProductResponse productRes = productService.createProduct(command);

        return createdResponse(productRes);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> details(@PathVariable UUID productId) {
        ProductResponse product = productService.getById(productId);

        return objectResponse(product);
    }

    @GetMapping("/product-variants/{productVariantId}")
    public ResponseEntity<Map<String, Object>> getProductVariant(@PathVariable UUID productVariantId) {
        ProductVariantResponse productVariant = productService.getProductVariant(productVariantId);

        return objectResponse(productVariant);
    }

    // Admins, or the vendor owning the product
    @PreAuthorize("@productSecurity.canManage(#productId)")
    @PutMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> update(
        @PathVariable UUID productId,
        @Valid @ModelAttribute UpdateProductRequest request
    ) throws IOException {
        UpdateProductCommand command = request.validated();
        ProductResponse updated = productService.updateProduct(productId, command);

        return objectResponse(updated);
    }

    // Admins, or the vendor owning the product
    @PreAuthorize("@productSecurity.canManage(#productId)")
    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable UUID productId) {
        productService.deleteProduct(productId);

        return successResponse("Product deleted successfully");
    }
}