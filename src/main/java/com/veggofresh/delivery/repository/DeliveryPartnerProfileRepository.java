package com.veggofresh.delivery.repository;

import com.veggofresh.delivery.entity.DeliveryKycStatus;
import com.veggofresh.delivery.entity.DeliveryPartnerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryPartnerProfileRepository extends JpaRepository<DeliveryPartnerProfile, UUID> {
    Optional<DeliveryPartnerProfile> findByUserId(UUID userId);
    List<DeliveryPartnerProfile> findByOnlineTrueAndKycStatus(DeliveryKycStatus kycStatus);

    /**
     * NEW THIS ROUND -- "awaiting review" means kycStatus PENDING AND onboarding fully
     * submitted (verificationStep == 3, per DeliveryPartnerProfile's own field comment).
     * PENDING alone isn't enough -- that also matches partners still mid-onboarding with
     * nothing yet to review. Powers DeliveryKycServiceImpl / Admin's KYC review queue.
     */
    Page<DeliveryPartnerProfile> findByKycStatusAndVerificationStep(DeliveryKycStatus kycStatus, int verificationStep, Pageable pageable);
}
