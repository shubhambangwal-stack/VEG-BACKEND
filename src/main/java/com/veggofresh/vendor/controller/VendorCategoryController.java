package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.vendor.dto.request.CategoryCreateRequestDto;
import com.veggofresh.vendor.dto.response.CategoryDto;
import com.veggofresh.vendor.entity.Category;
import com.veggofresh.vendor.repository.CategoryRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vendor/categories")
@RequiredArgsConstructor
public class VendorCategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategories() {
        List<CategoryDto> categories = categoryRepository.findAllByDeletedAtIsNull().stream()
                .map(cat -> CategoryDto.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .description(cat.getDescription())
                        .isActive(cat.isActive())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(categories, "Categories retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryDto>> addCategory(@Valid @RequestBody CategoryCreateRequestDto request) {
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isActive(true)
                .build();
        category = categoryRepository.save(category);

        CategoryDto dto = CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .isActive(category.isActive())
                .build();

        return ResponseEntity.ok(ApiResponse.success(dto, "Category added successfully"));
    }
}
