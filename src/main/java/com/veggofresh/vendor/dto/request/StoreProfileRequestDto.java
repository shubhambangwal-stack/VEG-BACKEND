package com.veggofresh.vendor.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Figma "Store Profile Management".
 *
 * <p>Bound via {@code @ModelAttribute} from {@code multipart/form-data} (not JSON) so the
 * store photo rides in the same call as the rest of the store profile fields.
 *
 * <p>{@code storeImage} is optional and single: omit it to leave the existing store photo
 * unchanged; include it to replace it (the old Cloudinary asset is auto-deleted). All other
 * fields keep their existing required/optional behavior from before this change --
 * only the image field's type changed, from a raw URL string to a real file upload.
 */
@Getter
@Setter
public class StoreProfileRequestDto {
    @NotBlank(message = "Store name is required")
    private String storeName;

    private String storeBio;

    /** Optional new store photo (jpg/jpeg/png/webp). Omit to leave the current one unchanged. */
    private MultipartFile storeImage;

    private List<String> attributes;

    @NotBlank(message = "Street address is required")
    private String streetAddress;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "ZIP/postal code is required")
    private String zipCode;

    private Double latitude;
    private Double longitude;
}
