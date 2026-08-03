package com.veggofresh.vendor.dto.response;

import com.veggofresh.vendor.entity.VendorDocumentStatus;
import com.veggofresh.vendor.entity.VendorDocumentType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class VendorDocumentResponseDto {
    private VendorDocumentType documentType;
    private VendorDocumentStatus status;
    private String fileUrl;
    private Instant updatedAt;
}
