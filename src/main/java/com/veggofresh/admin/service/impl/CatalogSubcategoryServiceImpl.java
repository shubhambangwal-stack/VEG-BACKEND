package com.veggofresh.admin.service.impl;

import com.veggofresh.admin.dto.request.SubcategoryRequestDto;
import com.veggofresh.admin.dto.response.SubcategoryResponseDto;
import com.veggofresh.admin.entity.CatalogCategory;
import com.veggofresh.admin.entity.CatalogSubcategory;
import com.veggofresh.admin.repository.CatalogCategoryRepository;
import com.veggofresh.admin.repository.CatalogSubcategoryRepository;
import com.veggofresh.admin.service.CatalogSubcategoryService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CatalogSubcategoryServiceImpl implements CatalogSubcategoryService {

    private final CatalogSubcategoryRepository subcategoryRepository;
    private final CatalogCategoryRepository categoryRepository;

    @Override
    public SubcategoryResponseDto createSubcategory(SubcategoryRequestDto request) {
        CatalogCategory category = getCategory(request.getCategoryId());
        if (subcategoryRepository.existsByNameIgnoreCaseAndCategoryId(request.getName(), category.getId())) {
            throw new BusinessException("CATALOG_SUBCATEGORY_DUPLICATE",
                    "A subcategory with this name already exists under this category", HttpStatus.CONFLICT);
        }
        CatalogSubcategory subcategory = new CatalogSubcategory();
        subcategory.setCategory(category);
        subcategory.setName(request.getName());
        subcategory.setDisplayOrder(request.getDisplayOrder());
        subcategory.setActive(true);
        return toDto(subcategoryRepository.save(subcategory));
    }

    @Override
    public SubcategoryResponseDto updateSubcategory(UUID id, SubcategoryRequestDto request) {
        CatalogSubcategory subcategory = getEntity(id);
        CatalogCategory category = getCategory(request.getCategoryId());

        boolean nameOrCategoryChanged = !subcategory.getName().equalsIgnoreCase(request.getName())
                || !subcategory.getCategory().getId().equals(category.getId());
        if (nameOrCategoryChanged
                && subcategoryRepository.existsByNameIgnoreCaseAndCategoryId(request.getName(), category.getId())) {
            throw new BusinessException("CATALOG_SUBCATEGORY_DUPLICATE",
                    "A subcategory with this name already exists under this category", HttpStatus.CONFLICT);
        }
        subcategory.setCategory(category);
        subcategory.setName(request.getName());
        subcategory.setDisplayOrder(request.getDisplayOrder());
        return toDto(subcategoryRepository.save(subcategory));
    }

    @Override
    @Transactional(readOnly = true)
    public SubcategoryResponseDto getSubcategoryById(UUID id) {
        return toDto(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubcategoryResponseDto> listByCategory(UUID categoryId) {
        Sort sort = Sort.by(Sort.Direction.ASC, "displayOrder");
        return subcategoryRepository.findAllByCategoryId(categoryId, sort)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubcategoryResponseDto> searchActiveSubcategories(UUID categoryId, String search, Pageable pageable) {
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        // Consistent with product/category search: an unknown-but-valid categoryId
        // returns an empty page (200), not a 404 -- no existence check here.
        return subcategoryRepository.searchActiveByCategory(categoryId, normalizedSearch, pageable).map(this::toDto);
    }

    @Override
    public SubcategoryResponseDto setActive(UUID id, boolean active) {
        CatalogSubcategory subcategory = getEntity(id);
        subcategory.setActive(active);
        return toDto(subcategoryRepository.save(subcategory));
    }

    private CatalogSubcategory getEntity(UUID id) {
        return subcategoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CATALOG_SUBCATEGORY_NOT_FOUND",
                        "Subcategory not found", HttpStatus.NOT_FOUND));
    }

    private CatalogCategory getCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException("CATALOG_CATEGORY_NOT_FOUND",
                        "Category not found", HttpStatus.NOT_FOUND));
    }

    private SubcategoryResponseDto toDto(CatalogSubcategory s) {
        return SubcategoryResponseDto.builder()
                .id(s.getId())
                .categoryId(s.getCategory().getId())
                .categoryName(s.getCategory().getName())
                .name(s.getName())
                .displayOrder(s.getDisplayOrder())
                .isActive(s.isActive())
                .build();
    }
}
