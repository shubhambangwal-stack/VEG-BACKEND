package com.veggofresh.delivery.repository;

import com.veggofresh.delivery.entity.DeliveryKycStatus;
import com.veggofresh.delivery.entity.DeliveryPartnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryPartnerProfileRepository extends JpaRepository<DeliveryPartnerProfile, UUID> {
    Optional<DeliveryPartnerProfile> findByUserId(UUID userId);
    List<DeliveryPartnerProfile> findByOnlineTrueAndKycStatus(DeliveryKycStatus kycStatus);
}
