package com.veggofresh.delivery.repository;

import com.veggofresh.delivery.entity.DeliveryOnlineSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryOnlineSessionRepository extends JpaRepository<DeliveryOnlineSession, UUID> {
    Optional<DeliveryOnlineSession> findByDeliveryPartnerUserIdAndEndedAtIsNull(UUID deliveryPartnerUserId);

    /** Sessions that overlap the given window at all -- caller clips start/end to the window when summing. */
    List<DeliveryOnlineSession> findByDeliveryPartnerUserIdAndStartedAtBefore(UUID deliveryPartnerUserId, Instant windowEnd);
}
