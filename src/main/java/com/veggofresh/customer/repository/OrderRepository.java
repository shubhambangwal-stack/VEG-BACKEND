package com.veggofresh.customer.repository;

import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderStatus;
import com.veggofresh.vendor.entity.VendorListing;
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

    // NOTE: unchanged this round — still a cross-module boundary violation
    // (direct JPQL reference to Vendor's Product entity). Flagged in
    // PROJECT_STATE, deliberately left alone until the catalog rework lands,
    // since fixing it properly means rethinking what "shopId" even means
    // once products aren't vendor-owned. See NOTES_CUSTOMER.md.
      
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i, VendorListing vl " +
           "WHERE i.productId = vl.catalogProductId AND vl.shop.id = :shopId")
    List<Order> findByShopId(@Param("shopId") UUID shopId);


  

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
