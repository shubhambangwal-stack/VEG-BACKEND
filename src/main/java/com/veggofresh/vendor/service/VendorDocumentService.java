package com.veggofresh.vendor.service;

import com.veggofresh.vendor.dto.response.VendorApplicationDetailsResponseDto;
import com.veggofresh.vendor.dto.response.VendorDocumentResponseDto;
import com.veggofresh.vendor.entity.VendorDocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface VendorDocumentService {
    /** Returns application details including all 3 document types for this vendor's shop, auto-creating PENDING rows for any missing type. */
    VendorApplicationDetailsResponseDto getDocuments(UUID ownerUserId);

    VendorDocumentResponseDto uploadDocument(UUID ownerUserId, VendorDocumentType type, MultipartFile file);
}
