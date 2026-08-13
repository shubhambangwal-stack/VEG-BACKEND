package com.veggofresh.admin.repository;

import com.veggofresh.admin.entity.CatalogCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

public interface CatalogCategoryRepository extends JpaRepository<CatalogCategory, UUID> {

    boolean existsByNameIgnoreCase(String name);

    List<CatalogCategory> findAllByIsActiveTrue(Sort sort);
}
