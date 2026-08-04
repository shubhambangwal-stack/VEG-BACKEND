package com.veggofresh.vendor.repository;

import com.veggofresh.vendor.entity.VendorSpecialClosure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorSpecialClosureRepository extends JpaRepository<VendorSpecialClosure, UUID> {
    List<VendorSpecialClosure> findByShopId(UUID shopId);
    Optional<VendorSpecialClosure> findByIdAndShopId(UUID id, UUID shopId);
}
