package com.veggofresh.delivery.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.util.UUID;

/**
 * One row per status transition an assignment goes through. BaseEntity.createdAt
 * IS the event timestamp -- no separate occurredAt field needed since rows are
 * immutable and only ever inserted, never updated.
 */
@Entity
@Table(name = "delivery_assignment_status_history")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class DeliveryAssignmentStatusHistory extends BaseEntity {

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DeliveryAssignmentStatus status;
}
