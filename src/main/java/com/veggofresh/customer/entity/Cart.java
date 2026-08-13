package com.veggofresh.customer.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * NEW ARCHITECTURE — multi-cart model (PROJECT_STATE section 2).
 *
 * A customer can have several concurrent OPEN carts. Each cart is
 * vendor-homogeneous by construction: every item in a given cart is
 * guaranteed to overlap on at least one vendor in {@link #candidateVendorIds}.
 * Carts are static once formed — no recompute/re-merge on item removal
 * (confirmed simplification, PROJECT_STATE section 2).
 *
 * PHASE 2 CHANGE: {@code userId} is no longer {@code unique = true}. See
 * NOTES_CUSTOMER.md for the migration this requires.
 */
@Entity
@Table(name = "carts")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class Cart extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "promo_code", length = 50)
    private String promoCode;

    @Column(name = "promo_discount", precision = 10, scale = 2)
    private BigDecimal promoDiscount;

    /**
     * Candidate vendor set for this cart — narrowed to the intersection every
     * time a new item joins. Computed live at add-time; re-validated fresh at
     * checkout time (PROJECT_STATE section 2, "Revisit-after-a-delay edge case").
     * Pre-pivot (single-vendor-per-product), this is always empty or a
     * single-element set; the narrowing logic itself needs no changes once
     * Vendor exposes true multi-vendor candidate sets.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cart_candidate_vendors", joinColumns = @JoinColumn(name = "cart_id"))
    @Column(name = "vendor_id")
    private Set<UUID> candidateVendorIds = new HashSet<>();
}
