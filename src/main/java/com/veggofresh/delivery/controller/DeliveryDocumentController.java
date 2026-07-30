package com.veggofresh.delivery.controller;

import com.veggofresh.delivery.dto.response.DeliveryDocumentResponseDto;
import com.veggofresh.delivery.entity.DeliveryDocumentType;
import com.veggofresh.delivery.service.DeliveryDocumentService;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/delivery/documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DELIVERY')")
public class DeliveryDocumentController {

    private final DeliveryDocumentService deliveryDocumentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeliveryDocumentResponseDto>>> getDocuments() {
        var documents = deliveryDocumentService.getDocuments(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(documents, "Documents retrieved successfully"));
    }

    @PostMapping("/{type}")
    public ResponseEntity<ApiResponse<DeliveryDocumentResponseDto>> uploadDocument(
            @PathVariable DeliveryDocumentType type,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate) {
        var document = deliveryDocumentService.uploadDocument(SecurityUtils.getCurrentUserId(), type, file, expiryDate);
        return ResponseEntity.ok(ApiResponse.success(document, "Document uploaded and pending review"));
    }
}
