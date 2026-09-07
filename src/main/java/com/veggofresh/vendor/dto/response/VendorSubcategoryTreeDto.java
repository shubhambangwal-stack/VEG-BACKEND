package com.veggofresh.vendor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** NEW -- see VendorCategoryTreeDto. products reuses VendorListingService.browseCatalog()'s
 *  existing mapping, so isListed and the (now-fixed) imageUrls gallery are both correct here too. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorSubcategoryTreeDto {
    private UUID id;
    private String name;
    private String imageUrl;
    private List<VendorListingDto> products;
}
