package com.veggofresh.delivery.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.UUID;

/**
 * One row per online/offline toggle. Started when DeliveryProfileServiceImpl.updateStatus
 * sets online=true, closed (endedAt set) when it's set back to false. Powers "Active
 * Hours"/"Work Hours" shown on the Earnings, Weekly Performance, and Profile Hub screens
 * -- nothing tracked this before this phase (only a single isOnline boolean existed).
 * endedAt == null means the session is still open (partner is currently online).
 */
@Entity
@Table(name = "delivery_online_sessions")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class DeliveryOnlineSession extends BaseEntity {

    @Column(name = "delivery_partner_user_id", nullable = false)
    private UUID deliveryPartnerUserId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;
}
