package com.veggofresh.delivery.service;

import com.veggofresh.delivery.dto.response.DeliveryDocumentResponseDto;
import com.veggofresh.delivery.entity.DeliveryDocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DeliveryDocumentService {
    /** Returns all 4 document types for this partner, auto-creating PENDING rows for any missing type. */
    List<DeliveryDocumentResponseDto> getDocuments(UUID userId);

    DeliveryDocumentResponseDto uploadDocument(UUID userId, DeliveryDocumentType type, MultipartFile file, LocalDate expiryDate);
}
