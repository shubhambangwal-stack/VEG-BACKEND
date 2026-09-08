package com.veggofresh.vendor.repository;

import com.veggofresh.vendor.entity.KycStatus;
import com.veggofresh.vendor.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopRepository extends JpaRepository<Shop, UUID> {
    Optional<Shop> findByOwnerUserIdAndDeletedAtIsNull(UUID ownerUserId);
    Optional<Shop> findByIdAndDeletedAtIsNull(UUID id);
    List<Shop> findAllByDeletedAtIsNullAndIsOnlineTrueAndKycStatus(KycStatus kycStatus);

    /**
     * NEW -- "awaiting review" means kycStatus PENDING AND the application has actually
     * been submitted (applicationSubmittedAt set) -- PENDING alone also matches shops
     * still mid-onboarding with nothing yet to review. Same distinction Delivery's
     * equivalent query makes. Powers VendorKycServiceImpl / Admin's KYC review queue.
     */
    Page<Shop> findByKycStatusAndApplicationSubmittedAtIsNotNull(KycStatus kycStatus, Pageable pageable);
}
