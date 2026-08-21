package com.veggofresh.customer.repository;

import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    // FIXED THIS ROUND: previously a cross-module boundary violation (direct JPQL
    // reference to Vendor's Product entity) that ALSO silently returned the wrong
    // orders under the multi-vendor catalog model -- it matched "this shop currently
    // sells ANY product in this order," not "this order was actually resolved to this
    // shop at checkout." Under the old single-vendor-per-product model those were the
    // same thing; they stopped being the same thing the moment two vendors could list
    // the same CatalogProduct. Symptom: if Vendor A and Vendor B both sell Tomatoes,
    // an order containing Tomatoes showed up as accept-able (and later "delivered") on
    // BOTH vendors' order lists, regardless of which one the multi-cart logic actually
    // narrowed the order to at checkout.
    //
    // Real fix: join through Order.candidateVendorIds -- the field OrderServiceImpl
    // .buildOrderFromCart() already populates at checkout with the live-revalidated,
    // narrowed vendor set for that specific order, but that nothing was reading until
    // now. Also resolves the separate issue that the old JPQL referenced Vendor's
    // Product entity, which no longer exists post-catalog-pivot (would fail at
    // Hibernate startup validation, not just return wrong data).
    @Query("SELECT DISTINCT o FROM Order o JOIN o.candidateVendorIds v WHERE v = :shopId")
    List<Order> findByShopId(@Param("shopId") UUID shopId);

    /**
     * NEW THIS ROUND -- real atomic accept for the vendor-accept race. When
     * candidateVendorIds legitimately holds more than one vendor (a cart that only
     * ever contained a product every candidate shares, never narrowed further), more
     * than one vendor can genuinely see the same order as accept-able. Same pattern as
     * Delivery's DeliveryAssignmentRepository.atomicClaim() -- one conditional UPDATE,
     * not a read-then-write, so only the first caller can actually flip status. Sets
     * confirmedAt in the same atomic statement rather than as a separate write.
     */
    @Modifying
    @Query("UPDATE Order o SET o.status = :newStatus, o.confirmedAt = CURRENT_TIMESTAMP " +
           "WHERE o.id = :orderId AND o.status = :expectedStatus")
    int atomicAccept(@Param("orderId") UUID orderId, @Param("newStatus") OrderStatus newStatus,
                      @Param("expectedStatus") OrderStatus expectedStatus);

    Page<Order> findByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    Page<Order> findByUserIdAndStatusIn(UUID userId, List<OrderStatus> statuses, Pageable pageable);

    long countByUserId(UUID userId);

    // ── Admin reporting queries ─────────────────────────────────────────────

    long countByUserIdAndStatus(UUID userId, OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.userId = :userId AND o.deletedAt IS NULL")
    java.math.BigDecimal sumTotalAmountByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(DISTINCT o.userId) FROM Order o WHERE " +
           "YEAR(o.createdAt) = YEAR(CURRENT_TIMESTAMP) AND MONTH(o.createdAt) = MONTH(CURRENT_TIMESTAMP) AND " +
           "o.deletedAt IS NULL")
    long countDistinctActiveCustomersThisMonth();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'DELIVERED' AND o.deletedAt IS NULL")
    java.math.BigDecimal sumTotalRevenue();

    long countByStatus(OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.deletedAt IS NULL ORDER BY o.createdAt DESC")
    Page<Order> findAllOrders(Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}
