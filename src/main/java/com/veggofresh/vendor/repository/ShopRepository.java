package com.veggofresh.vendor.repository;

import com.veggofresh.vendor.entity.KycStatus;
import com.veggofresh.vendor.entity.Shop;
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
}

