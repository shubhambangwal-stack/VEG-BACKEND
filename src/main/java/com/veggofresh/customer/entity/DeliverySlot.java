package com.veggofresh.customer.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

@Entity
@Table(name = "delivery_slots")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class DeliverySlot extends BaseEntity {

    @Column(name = "slot_date", nullable = false)
    private LocalDate date;

    /** e.g. "09:00" */
    @Column(name = "start_time", nullable = false, length = 5)
    private String startTime;

    /** e.g. "11:00" */
    @Column(name = "end_time", nullable = false, length = 5)
    private String endTime;

    /** e.g. "09:00 - 11:00" */
    @Column(nullable = false, length = 30)
    private String label;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = true;
}
