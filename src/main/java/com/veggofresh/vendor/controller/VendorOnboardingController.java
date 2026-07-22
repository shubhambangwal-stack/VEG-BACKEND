package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import com.veggofresh.vendor.dto.request.BusinessAddressRequestDto;
import com.veggofresh.vendor.dto.response.ApplicationStatusResponseDto;
import com.veggofresh.vendor.entity.KycStatus;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.repository.ShopRepository;
import com.veggofresh.platform.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/vendor")
@RequiredArgsConstructor
public class VendorOnboardingController {

    private final ShopRepository shopRepository;

    @PostMapping("/onboarding/address")
    public ResponseEntity<ApiResponse<Void>> submitBusinessAddress(@Valid @RequestBody BusinessAddressRequestDto request) {
        Shop shop = shopRepository.findByOwnerUserIdAndDeletedAtIsNull(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new BusinessException("SHOP_NOT_FOUND", "Vendor shop not found"));

        // Combine into single address string for now to match Shop entity
        String fullAddress = request.getStreet() + ", " + request.getCity() + ", " + request.getState() + " " + request.getZipCode();
        shop.setAddress(fullAddress);
        shopRepository.save(shop);

        return ResponseEntity.ok(ApiResponse.success("Address updated successfully"));
    }

    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadDocument(
            @RequestParam("type") String type,
            @RequestParam("file") MultipartFile file) {
        
        // Mock upload logic
        String fileName = type + "_" + System.currentTimeMillis() + ".pdf";
        return ResponseEntity.ok(ApiResponse.success(Map.of("file_name", fileName), "Document uploaded successfully"));
    }

    @GetMapping("/application/status")
    public ResponseEntity<ApiResponse<ApplicationStatusResponseDto>> getApplicationStatus() {
        Shop shop = shopRepository.findByOwnerUserIdAndDeletedAtIsNull(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new BusinessException("SHOP_NOT_FOUND", "Vendor shop not found"));

        String status = mapKycStatusToFlutterStatus(shop.getKycStatus());
        String declineReason = shop.getKycStatus() == KycStatus.REJECTED ? "Your provided documents did not pass verification." : null;

        ApplicationStatusResponseDto response = ApplicationStatusResponseDto.builder()
                .status(status)
                .declineReason(declineReason)
                .submittedAt(shop.getCreatedAt())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Application status retrieved"));
    }

    private String mapKycStatusToFlutterStatus(KycStatus kycStatus) {
        if (kycStatus == null) return "underReview";
        return switch (kycStatus) {
            case PENDING -> "underReview";
            case APPROVED -> "approved";
            case REJECTED -> "declined";
        };
    }
}
