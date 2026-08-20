package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.common.PageResponse;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.platform.security.SecurityUtils;
import com.veggofresh.vendor.dto.response.VendorListingDto;
import com.veggofresh.vendor.service.VendorListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * NEW ARCHITECTURE — replaces VendorProductController / VendorCategoryController.
 * A vendor no longer creates products or categories; they browse Admin's master
 * catalog and toggle isListed per item.
 *
 * <pre>
 * GET    /api/vendor/listings?search=&categoryId=&subcategoryId=&page=&size=
 *        — browse Admin's catalog with this vendor's current isListed flag merged in
 * PUT    /api/vendor/listings/{catalogProductId}?listed=true|false
 *        — toggle whether this vendor carries a catalog item. Row stays in "mine"
 *          either way -- unlisting only hides it from customers, it does not remove it.
 * GET    /api/vendor/listings/mine?status=ALL|LISTED|UNLISTED&page=&size=
 *        — everything this vendor has added, listed or not (default ALL)
 * DELETE /api/vendor/listings/{catalogProductId}
 *        — permanently removes the item from "mine" (soft delete). Works whether
 *          the item is currently listed or unlisted.
 * </pre>
 */
@RestController
@RequestMapping("/api/vendor/listings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorListingController {

    private final VendorListingService vendorListingService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VendorListingDto>>> browseCatalog(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID subcategoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<VendorListingDto> result = vendorListingService.browseCatalog(
                SecurityUtils.getCurrentUserId(), search, categoryId, subcategoryId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result), "Catalog retrieved successfully"));
    }

    @PutMapping("/{catalogProductId}")
    public ResponseEntity<ApiResponse<VendorListingDto>> setListed(
            @PathVariable UUID catalogProductId,
            @RequestParam boolean listed) {
        VendorListingDto result = vendorListingService.setListed(SecurityUtils.getCurrentUserId(), catalogProductId, listed);
        return ResponseEntity.ok(ApiResponse.success(result,
                listed ? "Item added to your storefront" : "Item removed from your storefront"));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<PageResponse<VendorListingDto>>> getMyListings(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Boolean isListedFilter = resolveStatusFilter(status);
        Page<VendorListingDto> result = vendorListingService.getMyListings(
                SecurityUtils.getCurrentUserId(), isListedFilter, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result), "Your listings retrieved successfully"));
    }

    @DeleteMapping("/{catalogProductId}")
    public ResponseEntity<ApiResponse<Void>> deleteListing(@PathVariable UUID catalogProductId) {
        vendorListingService.deleteListing(SecurityUtils.getCurrentUserId(), catalogProductId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from your storefront"));
    }

    private Boolean resolveStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        switch (status.trim().toUpperCase()) {
            case "LISTED":
                return Boolean.TRUE;
            case "UNLISTED":
                return Boolean.FALSE;
            case "ALL":
                return null;
            default:
                throw new BusinessException("INVALID_STATUS_FILTER",
                        "status must be one of ALL, LISTED, UNLISTED", HttpStatus.BAD_REQUEST);
        }
    }
}
