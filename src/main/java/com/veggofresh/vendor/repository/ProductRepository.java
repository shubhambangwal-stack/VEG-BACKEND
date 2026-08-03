package com.veggofresh.vendor.repository;

import com.veggofresh.vendor.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);
    Optional<Product> findByIdAndShopIdAndDeletedAtIsNull(UUID id, UUID shopId);
    List<Product> findAllByShopIdAndDeletedAtIsNull(UUID shopId);
    List<Product> findAllByShopIdAndIsActiveTrueAndDeletedAtIsNull(UUID shopId);

    @Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL AND p.isActive = true " +
           "AND p.shop.deletedAt IS NULL AND p.shop.isOnline = true AND p.shop.kycStatus = com.veggofresh.vendor.entity.KycStatus.APPROVED " +
           "AND (:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:category IS NULL OR LOWER(p.category.name) = LOWER(:category)) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> searchActiveProducts(
            @Param("query") String query,
            @Param("category") String category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
}
