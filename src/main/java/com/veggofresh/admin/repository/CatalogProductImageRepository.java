package com.veggofresh.admin.repository;

import com.veggofresh.admin.entity.CatalogProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogProductImageRepository extends JpaRepository<CatalogProductImage, UUID> {

    List<CatalogProductImage> findByProductIdOrderBySortOrderAsc(UUID productId);

    Optional<CatalogProductImage> findByIdAndProductId(UUID id, UUID productId);

    long countByProductId(UUID productId);

    void deleteByProductId(UUID productId);
}
