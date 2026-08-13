package com.veggofresh.admin.service;

import com.veggofresh.admin.dto.request.SubcategoryRequestDto;
import com.veggofresh.admin.dto.response.SubcategoryResponseDto;

import java.util.List;
import java.util.UUID;

public interface CatalogSubcategoryService {
    SubcategoryResponseDto createSubcategory(SubcategoryRequestDto request);
    SubcategoryResponseDto updateSubcategory(UUID id, SubcategoryRequestDto request);
    SubcategoryResponseDto getSubcategoryById(UUID id);
    List<SubcategoryResponseDto> listByCategory(UUID categoryId);
    SubcategoryResponseDto setActive(UUID id, boolean active);
}
