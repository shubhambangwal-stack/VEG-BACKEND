package com.veggofresh.admin.service.impl;

import com.veggofresh.admin.dto.response.CategoryResponseDto;
import com.veggofresh.admin.dto.response.CategoryTreeDto;
import com.veggofresh.admin.dto.response.ProductResponseDto;
import com.veggofresh.admin.dto.response.SubcategoryResponseDto;
import com.veggofresh.admin.dto.response.SubcategoryTreeDto;
import com.veggofresh.admin.service.AdminProductService;
import com.veggofresh.admin.service.CatalogCategoryService;
import com.veggofresh.admin.service.CatalogSubcategoryService;
import com.veggofresh.admin.service.CatalogTreeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogTreeServiceImpl implements CatalogTreeService {

    /** Overfetch size for tree building -- same pattern/caveat used elsewhere for this kind of composition. */
    private static final int OVERFETCH_SIZE = 2000;

    private final CatalogCategoryService catalogCategoryService;
    private final CatalogSubcategoryService catalogSubcategoryService;
    private final AdminProductService adminProductService;

    @Override
    public CategoryTreeDto getCategoryTree(UUID categoryId) {
        CategoryResponseDto category = catalogCategoryService.getCategoryById(categoryId);
        return buildCategoryTree(category);
    }

    @Override
    public List<CategoryTreeDto> getFullCatalogTree() {
        return catalogCategoryService.listCategories(false).stream()
                .map(this::buildCategoryTree)
                .collect(Collectors.toList());
    }

    private CategoryTreeDto buildCategoryTree(CategoryResponseDto category) {
        List<SubcategoryTreeDto> subcategoryTrees = catalogSubcategoryService.listByCategory(category.getId()).stream()
                .map(this::buildSubcategoryTree)
                .collect(Collectors.toList());

        return CategoryTreeDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .subcategories(subcategoryTrees)
                .build();
    }

    private SubcategoryTreeDto buildSubcategoryTree(SubcategoryResponseDto subcategory) {
        Page<ProductResponseDto> products = adminProductService.searchProducts(
                null, subcategory.getCategoryId(), subcategory.getId(), PageRequest.of(0, OVERFETCH_SIZE));

        return SubcategoryTreeDto.builder()
                .id(subcategory.getId())
                .name(subcategory.getName())
                .imageUrl(subcategory.getImageUrl())
                .products(products.getContent())
                .build();
    }
}
