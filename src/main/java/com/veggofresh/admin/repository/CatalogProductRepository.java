package com.veggofresh.admin.repository;

import com.veggofresh.admin.entity.CatalogProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CatalogProductRepository extends JpaRepository<CatalogProduct, UUID> {

    boolean existsByNameIgnoreCaseAndSubcategoryId(String name, UUID subcategoryId);

    @Query("SELECT p FROM CatalogProduct p WHERE " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:subcategoryId IS NULL OR p.subcategory.id = :subcategoryId)")
    Page<CatalogProduct> search(@Param("search") String search,
                                 @Param("categoryId") UUID categoryId,
                                 @Param("subcategoryId") UUID subcategoryId,
                                 Pageable pageable);
}
