package com.veggofresh.admin.repository;

import com.veggofresh.admin.entity.CatalogCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CatalogCategoryRepository extends JpaRepository<CatalogCategory, UUID> {

    boolean existsByNameIgnoreCase(String name);

    List<CatalogCategory> findAllByIsActiveTrue(Sort sort);

    /**
     * Paginated, searchable, active-only categories -- used by the vendor-facing
     * category picker (VendorCategoryController) so a vendor can type-to-search
     * instead of scrolling a flat dropdown. :search uses an explicit CAST to
     * string so Postgres doesn't fall back to inferring `bytea` for the bind
     * parameter when it's null (see CatalogProductRepository.search for the
     * same fix applied to product search).
     */
    @Query("SELECT c FROM CatalogCategory c WHERE " +
           "c.isActive = true AND " +
           "(:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<CatalogCategory> searchActive(@Param("search") String search, Pageable pageable);
}
