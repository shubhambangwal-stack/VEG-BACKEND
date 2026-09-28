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
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
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
 * vendor-homogeneous by construction: its items are all fulfillable by at least
 * one vendor in {@link #candidateVendorIds}.
 *
 * {@code userId} is NOT unique — one customer may hold several carts at once
 * (PHASE 2; see the V47/V113 migrations that dropped the old constraint).
 *
 * <h2>candidateVendorIds is derived state, not configuration</h2>
 *
 * It is always re-derived from the cart's items' live vendor sets via
 * {@code CartVendorResolver.Session#resync} after any line-item change. It is
 * persisted (rather than recomputed on demand) so the overlap is inspectable in
 * the database and available to queries, but it must never be treated as
 * authoritative input: an older version narrowed it destructively on every add
 * and never revisited it on removal, which could only shrink it and caused
 * spurious extra carts. See CartVendorResolver for the full failure mode.
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
    @BatchSize(size = 50)
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "promo_code", length = 50)
    private String promoCode;

    @Column(name = "promo_discount", precision = 10, scale = 2)
    private BigDecimal promoDiscount;

    /**
     * The set of vendors that can fulfill every item in this cart, i.e. the
     * intersection of the items' vendor sets. Empty means the cart can no longer
     * ship as a single order and checkout will report it as an issue.
     *
     * <p>Derived state — see the class javadoc. Mutate it only through
     * {@code CartVendorResolver.Session#resync}.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cart_candidate_vendors", joinColumns = @JoinColumn(name = "cart_id"))
    @Column(name = "vendor_id")
    private Set<UUID> candidateVendorIds = new HashSet<>();

    /**
     * Whether this cart holds anything at all. Null-safe on the collection.
     *
     * <p>An open cart with no items is a ghost and must never be shown to the
     * customer or priced: it renders as an extra "Cart N" card, shifts the
     * labels of every cart after it, and can absorb a later add that resurrects
     * it along with its stale promo code. Writers must retire such carts (see
     * CartServiceImpl) and readers must skip them.
     */
    @Transient
    public boolean hasItems() {
        return items != null && !items.isEmpty();
    }
}
