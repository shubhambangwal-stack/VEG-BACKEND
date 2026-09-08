package com.veggofresh.vendor.service.impl;

import com.veggofresh.admin.dto.response.CategoryResponseDto;
import com.veggofresh.admin.dto.response.SubcategoryResponseDto;
import com.veggofresh.admin.service.CatalogCategoryService;
import com.veggofresh.admin.service.CatalogSubcategoryService;
import com.veggofresh.vendor.service.VendorCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorCategoryServiceImpl implements VendorCategoryService {

    private final CatalogCategoryService catalogCategoryService;
    private final CatalogSubcategoryService catalogSubcategoryService;

    @Override
    public Page<CategoryResponseDto> browseCategories(String search, Pageable pageable) {
        return catalogCategoryService.searchActiveCategories(search, pageable);
    }

    @Override
    public Page<SubcategoryResponseDto> browseSubcategories(UUID categoryId, String search, Pageable pageable) {
        return catalogSubcategoryService.searchActiveSubcategories(categoryId, search, pageable);
    }
}
