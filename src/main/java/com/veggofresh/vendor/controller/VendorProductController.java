package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import com.veggofresh.vendor.dto.request.ProductCreateRequestDto;
import com.veggofresh.vendor.dto.request.ProductUpdateRequestDto;
import com.veggofresh.vendor.dto.response.ProductDto;
import com.veggofresh.vendor.service.VendorProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendor/products")
@RequiredArgsConstructor
public class VendorProductController {

    private final VendorProductService vendorProductService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDto>>> getProducts() {
        return ResponseEntity.ok(ApiResponse.success(vendorProductService.getProductsByShop(SecurityUtils.getCurrentUserId()), "Products retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDto>> addProduct(@Valid @RequestBody ProductCreateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(vendorProductService.addProduct(SecurityUtils.getCurrentUserId(), request), "Product added successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(vendorProductService.updateProduct(SecurityUtils.getCurrentUserId(), id, request), "Product updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        vendorProductService.deleteProduct(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<ApiResponse<ProductDto>> uploadProductImage(@PathVariable UUID id, @RequestBody Map<String, String> request) {
        String imageUrl = request.get("imageUrl");
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = "https://veggofresh-images.s3.amazonaws.com/products/default.png";
        }
        ProductDto updated = vendorProductService.updateProductImage(SecurityUtils.getCurrentUserId(), id, imageUrl);
        return ResponseEntity.ok(ApiResponse.success(updated, "Product image updated successfully"));
    }
}
