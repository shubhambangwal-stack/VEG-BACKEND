package com.veggofresh.admin.repository;

import com.veggofresh.admin.entity.CatalogSubcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

public interface CatalogSubcategoryRepository extends JpaRepository<CatalogSubcategory, UUID> {

    List<CatalogSubcategory> findAllByCategoryId(UUID categoryId, Sort sort);

    boolean existsByNameIgnoreCaseAndCategoryId(String name, UUID categoryId);
}
