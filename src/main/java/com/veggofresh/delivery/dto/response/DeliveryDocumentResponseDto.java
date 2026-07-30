package com.veggofresh.delivery.dto.response;

import com.veggofresh.delivery.entity.DeliveryDocumentStatus;
import com.veggofresh.delivery.entity.DeliveryDocumentType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
public class DeliveryDocumentResponseDto {
    private DeliveryDocumentType documentType;
    private DeliveryDocumentStatus status;
    private String fileUrl;
    private LocalDate expiryDate;
    private Instant updatedAt;
}
