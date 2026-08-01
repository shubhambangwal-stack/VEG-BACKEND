package com.veggofresh.vendor.service;

import org.springframework.web.multipart.MultipartFile;

import com.veggofresh.vendor.dto.response.VendorDocumentResponseDto;
import com.veggofresh.vendor.entity.VendorDocumentType;

import java.util.List;
import java.util.UUID;

public interface VendorDocumentService {
    /** Returns all 3 document types for this vendor's shop, auto-creating PENDING rows for any missing type. */
    List<VendorDocumentResponseDto> getDocuments(UUID ownerUserId);

    VendorDocumentResponseDto uploadDocument(UUID ownerUserId, VendorDocumentType type, MultipartFile file);
}
