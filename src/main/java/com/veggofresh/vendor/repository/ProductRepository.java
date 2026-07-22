package com.veggofresh.vendor.repository;

import com.veggofresh.vendor.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);
    Optional<Product> findByIdAndShopIdAndDeletedAtIsNull(UUID id, UUID shopId);
    List<Product> findAllByShopIdAndDeletedAtIsNull(UUID shopId);
    List<Product> findAllByShopIdAndIsActiveTrueAndDeletedAtIsNull(UUID shopId);
}
