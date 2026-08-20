package com.veggofresh.admin.service;

import com.veggofresh.admin.dto.request.CategoryRequestDto;
import com.veggofresh.admin.dto.response.CategoryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CatalogCategoryService {
    CategoryResponseDto createCategory(CategoryRequestDto request);
    CategoryResponseDto updateCategory(UUID id, CategoryRequestDto request);
    CategoryResponseDto getCategoryById(UUID id);
    List<CategoryResponseDto> listCategories(boolean includeInactive);
    CategoryResponseDto setActive(UUID id, boolean active);

    /**
     * Paginated, searchable, active-only categories. Added for the vendor
     * category picker -- does not replace listCategories(), which Admin's own
     * screens keep using unchanged.
     */
    Page<CategoryResponseDto> searchActiveCategories(String search, Pageable pageable);
}
