package com.veggofresh.admin.service.impl;

import com.veggofresh.admin.dto.request.CategoryRequestDto;
import com.veggofresh.admin.dto.response.CategoryResponseDto;
import com.veggofresh.admin.entity.CatalogCategory;
import com.veggofresh.admin.repository.CatalogCategoryRepository;
import com.veggofresh.admin.service.CatalogCategoryService;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.platform.storage.CloudinaryService;
import com.veggofresh.platform.storage.CloudinaryUploadResult;
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
public class CatalogCategoryServiceImpl implements CatalogCategoryService {

    private final CatalogCategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    public CategoryResponseDto createCategory(CategoryRequestDto request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException("CATALOG_CATEGORY_DUPLICATE",
                    "A category with this name already exists", HttpStatus.CONFLICT);
        }
        CatalogCategory category = new CatalogCategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setDisplayOrder(request.getDisplayOrder());
        category.setActive(true);

        // Image is optional at creation too -- omit it and add one later via update.
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            CloudinaryUploadResult upload = cloudinaryService.uploadImage(
                    request.getImage(), "veggofresh/catalog/categories");
            category.setImageUrl(upload.url());
            category.setImagePublicId(upload.publicId());
        }

        return toDto(categoryRepository.save(category));
    }

    @Override
    public CategoryResponseDto updateCategory(UUID id, CategoryRequestDto request) {
        CatalogCategory category = getEntity(id);
        if (!category.getName().equalsIgnoreCase(request.getName())
                && categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException("CATALOG_CATEGORY_DUPLICATE",
                    "A category with this name already exists", HttpStatus.CONFLICT);
        }
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setDisplayOrder(request.getDisplayOrder());

        // Image: optional, single. Omitting it leaves the current image untouched --
        // this does NOT follow the "always overwrite" behavior of the fields above.
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            CloudinaryUploadResult upload = cloudinaryService.uploadImage(
                    request.getImage(), "veggofresh/catalog/categories");
            String oldPublicId = category.getImagePublicId();
            category.setImageUrl(upload.url());
            category.setImagePublicId(upload.publicId());
            cloudinaryService.deleteQuietly(oldPublicId);
        }

        return toDto(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(UUID id) {
        return toDto(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> listCategories(boolean includeInactive) {
        Sort sort = Sort.by(Sort.Direction.ASC, "displayOrder");
        List<CatalogCategory> categories = includeInactive
                ? categoryRepository.findAll(sort)
                : categoryRepository.findAllByIsActiveTrue(sort);
        return categories.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponseDto> searchActiveCategories(String search, Pageable pageable) {
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return categoryRepository.searchActive(normalizedSearch, pageable).map(this::toDto);
    }

    @Override
    public CategoryResponseDto setActive(UUID id, boolean active) {
        CatalogCategory category = getEntity(id);
        category.setActive(active);
        return toDto(categoryRepository.save(category));
    }

    private CatalogCategory getEntity(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CATALOG_CATEGORY_NOT_FOUND",
                        "Category not found", HttpStatus.NOT_FOUND));
    }

    private CategoryResponseDto toDto(CatalogCategory c) {
        return CategoryResponseDto.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .imageUrl(c.getImageUrl())
                .displayOrder(c.getDisplayOrder())
                .isActive(c.isActive())
                .build();
    }
}
