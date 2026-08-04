package com.veggofresh.vendor.repository;

import com.veggofresh.vendor.entity.VendorOperatingHour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorOperatingHourRepository extends JpaRepository<VendorOperatingHour, UUID> {
    List<VendorOperatingHour> findByShopId(UUID shopId);
    Optional<VendorOperatingHour> findByShopIdAndDayOfWeek(UUID shopId, DayOfWeek dayOfWeek);
}
