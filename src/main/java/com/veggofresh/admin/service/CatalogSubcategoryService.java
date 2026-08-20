package com.veggofresh.admin.service;

import com.veggofresh.admin.dto.request.SubcategoryRequestDto;
import com.veggofresh.admin.dto.response.SubcategoryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CatalogSubcategoryService {
    SubcategoryResponseDto createSubcategory(SubcategoryRequestDto request);
    SubcategoryResponseDto updateSubcategory(UUID id, SubcategoryRequestDto request);
    SubcategoryResponseDto getSubcategoryById(UUID id);
    List<SubcategoryResponseDto> listByCategory(UUID categoryId);
    SubcategoryResponseDto setActive(UUID id, boolean active);

    /**
     * Paginated, searchable, active-only subcategories scoped to one category.
     * Added for the vendor subcategory picker -- does not replace
     * listByCategory(), which Admin's own screens keep using unchanged.
     */
    Page<SubcategoryResponseDto> searchActiveSubcategories(UUID categoryId, String search, Pageable pageable);
}
