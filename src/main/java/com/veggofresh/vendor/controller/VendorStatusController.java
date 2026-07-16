package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.vendor.service.VendorShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendor/status")
@RequiredArgsConstructor
public class VendorStatusController {

    private final VendorShopService vendorShopService;

    private UUID getCurrentUserId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> setShopStatus(@RequestBody Map<String, Boolean> statusMap) {
        Boolean isOnline = statusMap.getOrDefault("isOnline", false);
        vendorShopService.setShopStatus(getCurrentUserId(), isOnline);
        return ResponseEntity.ok(ApiResponse.success("Shop status updated successfully"));
    }
}
