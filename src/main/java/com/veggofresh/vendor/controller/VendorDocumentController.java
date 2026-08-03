package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import com.veggofresh.vendor.dto.response.VendorDocumentResponseDto;
import com.veggofresh.vendor.entity.VendorDocumentType;
import com.veggofresh.vendor.service.VendorDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Serves BOTH onboarding Step 3 (each Figma "Upload" button hits this directly,
 * per document type) AND later standalone re-uploads (e.g. renewing an expired
 * license) -- one endpoint, no duplication needed since there's no structured
 * text data paired with these documents (unlike Delivery's license number /
 * vehicle details, which needed merging with their photo uploads).
 */
@RestController
@RequestMapping("/api/vendor/documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorDocumentController {

    private final VendorDocumentService vendorDocumentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VendorDocumentResponseDto>>> getDocuments() {
        var documents = vendorDocumentService.getDocuments(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(documents, "Documents retrieved successfully"));
    }

    @PostMapping("/{type}")
    public ResponseEntity<ApiResponse<VendorDocumentResponseDto>> uploadDocument(
            @PathVariable VendorDocumentType type,
            @RequestParam("file") MultipartFile file) {
        var document = vendorDocumentService.uploadDocument(SecurityUtils.getCurrentUserId(), type, file);
        return ResponseEntity.ok(ApiResponse.success(document, "Document uploaded successfully"));
    }
}
