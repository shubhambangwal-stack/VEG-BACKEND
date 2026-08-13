package com.veggofresh.admin.service;

import com.veggofresh.admin.dto.request.CategoryRequestDto;
import com.veggofresh.admin.dto.response.CategoryResponseDto;

import java.util.List;
import java.util.UUID;

public interface CatalogCategoryService {
    CategoryResponseDto createCategory(CategoryRequestDto request);
    CategoryResponseDto updateCategory(UUID id, CategoryRequestDto request);
    CategoryResponseDto getCategoryById(UUID id);
    List<CategoryResponseDto> listCategories(boolean includeInactive);
    CategoryResponseDto setActive(UUID id, boolean active);
}
