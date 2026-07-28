package com.veggofresh.customer.repository;

import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i, com.veggofresh.vendor.entity.Product p " +
           "WHERE i.productId = p.id AND p.shop.id = :shopId")
    List<Order> findByShopId(@Param("shopId") UUID shopId);

    // GAP 12 — status filtering for order history screen
    Page<Order> findByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    Page<Order> findByUserIdAndStatusIn(UUID userId, List<OrderStatus> statuses, Pageable pageable);

    // Count for profile summary
    long countByUserId(UUID userId);

    // ── Admin reporting queries ─────────────────────────────────────────────

    /** Count orders in a specific status (e.g. DELIVERED) for a customer */
    long countByUserIdAndStatus(UUID userId, OrderStatus status);

    /** Sum of all totalAmount for a customer — lifetime outlay */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.userId = :userId AND o.deletedAt IS NULL")
    java.math.BigDecimal sumTotalAmountByUserId(@Param("userId") UUID userId);

    /** Count customers who placed at least one order in the current month */
    @Query("SELECT COUNT(DISTINCT o.userId) FROM Order o WHERE " +
           "YEAR(o.createdAt) = YEAR(CURRENT_TIMESTAMP) AND MONTH(o.createdAt) = MONTH(CURRENT_TIMESTAMP) AND " +
           "o.deletedAt IS NULL")
    long countDistinctActiveCustomersThisMonth();

    /** Sum total revenue across all DELIVERED orders */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'DELIVERED' AND o.deletedAt IS NULL")
    java.math.BigDecimal sumTotalRevenue();

    /** Count all orders — for admin dashboard */
    long countByStatus(OrderStatus status);

    /** Paginated all orders for admin monitoring */
    @Query("SELECT o FROM Order o WHERE o.deletedAt IS NULL ORDER BY o.createdAt DESC")
    Page<Order> findAllOrders(Pageable pageable);

    /** Paginated filtered orders for admin monitoring */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}

