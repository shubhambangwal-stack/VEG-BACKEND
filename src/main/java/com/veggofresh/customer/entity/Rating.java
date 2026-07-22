package com.veggofresh.customer.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "ratings")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class Rating extends BaseEntity {

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "rating_value", nullable = false)
    private int ratingValue;

    @Column(length = 1000)
    private String comment;
}
