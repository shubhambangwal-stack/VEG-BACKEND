package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.vendor.dto.request.ProductCreateRequestDto;
import com.veggofresh.vendor.dto.request.ProductUpdateRequestDto;
import com.veggofresh.vendor.dto.response.ProductDto;
import com.veggofresh.vendor.service.VendorProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendor/products")
@RequiredArgsConstructor
public class VendorProductController {

    private final VendorProductService vendorProductService;

    private UUID getCurrentUserId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDto>>> getProducts() {
        return ResponseEntity.ok(ApiResponse.success(vendorProductService.getProductsByShop(getCurrentUserId()), "Products retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDto>> addProduct(@Valid @RequestBody ProductCreateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(vendorProductService.addProduct(getCurrentUserId(), request), "Product added successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(vendorProductService.updateProduct(getCurrentUserId(), id, request), "Product updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        vendorProductService.deleteProduct(getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<ApiResponse<Void>> uploadProductImage(@PathVariable UUID id) {
        // Mock image upload
        return ResponseEntity.ok(ApiResponse.success("Product image uploaded successfully"));
    }
}
