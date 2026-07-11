package com.veggofresh.platform.common;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standardized pagination response wrapper for all paginated VegGo Fresh endpoints.
 *
 * <p>Converts Spring Data's {@link Page} into a predictable, module-agnostic JSON shape:
 * <pre>
 * {
 *   "content":       [ ... ],
 *   "page":          0,
 *   "size":          20,
 *   "totalElements": 150,
 *   "totalPages":    8
 * }
 * </pre>
 *
 * <p>Note: {@code page} is 0-indexed to align with Spring Data conventions.
 * API consumers should document this clearly.
 *
 * <h3>Usage in controllers</h3>
 * <pre>{@code
 * Page<VendorDto> vendors = vendorService.findAll(pageable);
 * return ResponseEntity.ok(
 *     ApiResponse.success(PageResponse.of(vendors), "Vendors retrieved")
 * );
 * }</pre>
 *
 * @param <T> the type of elements in the page content
 */
@Getter
public class PageResponse<T> {

    /** The list of items in the current page. */
    private final List<T> content;

    /** Current page number (0-indexed). */
    private final int page;

    /** Number of items per page as requested. */
    private final int size;

    /** Total number of items across all pages. */
    private final long totalElements;

    /** Total number of pages available. */
    private final int totalPages;

    private PageResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    /**
     * Converts a Spring Data {@link Page} into a {@link PageResponse}.
     *
     * @param springPage the Spring Data page result
     * @param <T>        the element type
     * @return a new {@link PageResponse} populated from the given page
     */
    public static <T> PageResponse<T> of(Page<T> springPage) {
        return new PageResponse<>(
                springPage.getContent(),
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages()
        );
    }
}
