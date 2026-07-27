package com.veggofresh.customer.repository;

import com.veggofresh.customer.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByUserId(UUID userId, Pageable pageable);
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i, Product p WHERE i.productId = p.id AND p.shop.id = :shopId")
    List<Order> findByShopId(@Param("shopId") UUID shopId);
}
