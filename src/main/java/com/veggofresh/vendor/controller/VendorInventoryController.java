package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import com.veggofresh.vendor.dto.request.InventoryUpdateRequestDto;
import com.veggofresh.vendor.dto.response.InventoryItemDto;
import com.veggofresh.vendor.service.VendorInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/vendor/inventory")
@RequiredArgsConstructor
public class VendorInventoryController {

    private final VendorInventoryService vendorInventoryService;

    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryItemDto>> updateInventory(@PathVariable UUID productId, @Valid @RequestBody InventoryUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(vendorInventoryService.updateStock(SecurityUtils.getCurrentUserId(), productId, request), "Inventory updated successfully"));
    }
}
