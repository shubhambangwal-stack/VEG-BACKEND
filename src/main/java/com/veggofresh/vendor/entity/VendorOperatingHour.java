package com.veggofresh.vendor.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * One row per day of week per shop (7 rows total). Uses java.time.DayOfWeek directly
 * rather than a custom enum. openTime/closeTime are null when isOpen=false.
 */
@Entity
@Table(name = "vendor_operating_hours")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class VendorOperatingHour extends BaseEntity {

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(name = "is_open", nullable = false)
    private boolean isOpen;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;
}
