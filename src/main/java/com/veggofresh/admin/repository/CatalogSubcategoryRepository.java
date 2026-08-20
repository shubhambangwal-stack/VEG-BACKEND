package com.veggofresh.admin.repository;

import com.veggofresh.admin.entity.CatalogSubcategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CatalogSubcategoryRepository extends JpaRepository<CatalogSubcategory, UUID> {

    List<CatalogSubcategory> findAllByCategoryId(UUID categoryId, Sort sort);

    boolean existsByNameIgnoreCaseAndCategoryId(String name, UUID categoryId);

    /**
     * Paginated, searchable, active-only subcategories scoped to one category --
     * mirrors CatalogCategoryRepository.searchActive. Same explicit CAST fix on
     * :search to avoid the null-bind-parameter `bytea` issue in Postgres.
     */
    @Query("SELECT s FROM CatalogSubcategory s WHERE " +
           "s.category.id = :categoryId AND " +
           "s.isActive = true AND " +
           "(:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<CatalogSubcategory> searchActiveByCategory(@Param("categoryId") UUID categoryId,
                                                     @Param("search") String search,
                                                     Pageable pageable);
}
