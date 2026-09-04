package com.veggofresh.vendor.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.util.UUID;

/**
 * One row per document type per shop (BUSINESS_LICENSE, TAX_ID, GOVERNMENT_ID).
 * Same pattern as Delivery's DeliveryDocument. Used both during onboarding Step 3
 * and for later standalone re-uploads (e.g. renewing an expired license) --
 * unlike Delivery, there's no structured text data paired with these documents,
 * so one generic upload endpoint serves both purposes without needing a merge.
 */
@Entity
@Table(name = "vendor_documents")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class VendorDocument extends BaseEntity {

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private VendorDocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VendorDocumentStatus status = VendorDocumentStatus.PENDING;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    /**
     * Cloudinary public_id for {@link #fileUrl}, required to delete the old asset when
     * this document type is re-uploaded (e.g. renewing an expired license). Internal
     * only -- never exposed on VendorDocumentResponseDto.
     */
    @Column(name = "public_id", length = 500)
    private String publicId;
}
