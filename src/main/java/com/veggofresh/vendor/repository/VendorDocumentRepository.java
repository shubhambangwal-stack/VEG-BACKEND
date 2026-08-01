package com.veggofresh.vendor.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veggofresh.vendor.entity.VendorDocument;
import com.veggofresh.vendor.entity.VendorDocumentType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorDocumentRepository extends JpaRepository<VendorDocument, UUID> {
    List<VendorDocument> findByShopId(UUID shopId);
    Optional<VendorDocument> findByShopIdAndDocumentType(UUID shopId, VendorDocumentType documentType);
}
