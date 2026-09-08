package com.veggofresh.customer.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request for {@code PUT /api/customer/profile}.
 *
 * <p>Bound via {@code @ModelAttribute} from a single {@code multipart/form-data} request,
 * NOT a JSON body — this lets the client update name, email, and avatar together in one
 * call. All three fields are optional and behave as PATCH semantics: only fields actually
 * present in the request are applied; anything omitted is left unchanged.
 *
 * <p>Avatar is intentionally a real file upload, not a raw URL string. Passing a bare
 * URL was removed on purpose: without going through Cloudinary there is no
 * {@code public_id} to track, which breaks auto-delete-of-the-old-image-on-replace and
 * lets arbitrary, unvalidated URLs into the field.
 */
@Getter
@Setter
public class CustomerProfileUpdateRequestDto {

    @Size(min = 1, max = 100, message = "Full name must be between 1 and 100 characters")
    private String fullName;

    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    /** Optional new avatar image (jpg/jpeg/png/webp). Omit this part to leave the avatar unchanged. */
    private MultipartFile avatar;
}
