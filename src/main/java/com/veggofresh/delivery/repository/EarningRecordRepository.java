package com.veggofresh.delivery.repository;

import com.veggofresh.delivery.entity.EarningRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EarningRecordRepository extends JpaRepository<EarningRecord, UUID> {
    List<EarningRecord> findByDeliveryPartnerUserIdAndCreatedAtBetween(UUID deliveryPartnerUserId, Instant from, Instant to);
}
