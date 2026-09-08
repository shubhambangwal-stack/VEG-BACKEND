package com.veggofresh.platform.storage;

import com.cloudinary.Cloudinary;
import com.veggofresh.platform.exception.BusinessException;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Single shared entry point for every image/document upload in VegGo Fresh.
 *
 * <p>This REPLACES the old per-module mock storage classes ({@code MockFileStorageService}
 * in Delivery and {@code VendorMockFileStorageService} in Vendor) which only logged the
 * upload and returned a fake {@code mock://...} URL. All uploads across Customer, Vendor,
 * Delivery, and Admin now go through this class and land in real Cloudinary storage.
 *
 * <p>Two entry points are provided, with different validation rules:
 * <ul>
 *   <li>{@link #uploadImage} — profile photos, store/category/subcategory icons, product
 *       photos. Strictly images only (jpg/jpeg/png/webp).</li>
 *   <li>{@link #uploadDocument} — KYC/verification documents (vendor documents, delivery
 *       documents, onboarding license/insurance photos, proof-of-delivery). Accepts images
 *       plus PDF, since verification documents are commonly scanned as PDF.</li>
 * </ul>
 *
 * <h3>Deletion</h3>
 * Every upload returns a {@link CloudinaryUploadResult#publicId()} that MUST be persisted
 * by the caller (in a sibling {@code *_public_id} column) so the asset can later be removed
 * via {@link #delete(String)} or {@link #deleteQuietly(String)} when the image is replaced.
 *
 * <p>Note: Cloudinary classifies PDFs under {@code resource_type=image} (it can render PDF
 * pages as images), so a single {@code resource_type=image} is used consistently for both
 * uploads (via {@code resource_type=auto}, which Cloudinary resolves to {@code image} for
 * both pictures and PDFs) and deletes — no separate resource-type bookkeeping is needed.
 *
 * <h3>Configuration (application.yml)</h3>
 * <pre>
 * veggofresh:
 *   uploads:
 *     image:
 *       max-size-mb: 5
 *     document:
 *       max-size-mb: 10
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "pdf");

    /** Cloudinary classifies both images and PDFs as resource_type "image" — used for delete calls. */
    private static final String DELETE_RESOURCE_TYPE = "image";

    private final Cloudinary cloudinary;

    @Value("${veggofresh.uploads.image.max-size-mb:5}")
    private long imageMaxSizeMb;

    @Value("${veggofresh.uploads.document.max-size-mb:10}")
    private long documentMaxSizeMb;

    /**
     * Uploads a single display image: avatar, store photo, category/subcategory icon,
     * or a product photo. Restricted to jpg/jpeg/png/webp.
     *
     * @param file   the multipart image file (must not be null/empty)
     * @param folder the Cloudinary folder to upload into, e.g. {@code "veggofresh/customers/{userId}/avatar"}
     * @return the uploaded asset's URL and public_id
     * @throws BusinessException if the file is missing, the wrong type, or too large
     */
    public CloudinaryUploadResult uploadImage(MultipartFile file, String folder) {
        validate(file, IMAGE_EXTENSIONS, imageMaxSizeMb, "IMAGE");
        return doUpload(file, folder, "image");
    }

    /**
     * Uploads a KYC/verification document: vendor documents, delivery documents,
     * onboarding license/insurance photos, or a proof-of-delivery photo.
     * Accepts jpg/jpeg/png/webp/pdf.
     *
     * @param file   the multipart file (must not be null/empty)
     * @param folder the Cloudinary folder to upload into, e.g. {@code "veggofresh/vendor-documents/{shopId}"}
     * @return the uploaded asset's URL and public_id
     * @throws BusinessException if the file is missing, the wrong type, or too large
     */
    public CloudinaryUploadResult uploadDocument(MultipartFile file, String folder) {
        validate(file, DOCUMENT_EXTENSIONS, documentMaxSizeMb, "DOCUMENT");
        return doUpload(file, folder, "auto");
    }

    /**
     * Deletes an asset by its Cloudinary public_id. Throws if the delete call fails.
     * Use this only when the delete failing should itself fail the calling operation;
     * for the common "replace old image" case, prefer {@link #deleteQuietly(String)}.
     *
     * @param publicId Cloudinary public_id to delete; a null/blank id is a no-op
     */
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId,
                    ObjectUtils.asMap("resource_type", DELETE_RESOURCE_TYPE));
            log.info("Cloudinary delete result for {}: {}", publicId, result);
        } catch (IOException e) {
            log.error("Failed to delete Cloudinary asset {}", publicId, e);
            throw new BusinessException("IMAGE_DELETE_FAILED", "Failed to delete existing image", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Best-effort delete for the "replace old image with new one" flow. A failure here is
     * logged and swallowed — it must never cause the new upload (which has already
     * succeeded and already been saved) to be reported as a failure to the client.
     *
     * @param publicId Cloudinary public_id to delete; a null/blank id is a no-op
     */
    public void deleteQuietly(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", DELETE_RESOURCE_TYPE));
        } catch (Exception e) {
            log.warn("Non-fatal: failed to delete old Cloudinary asset {} — continuing anyway", publicId, e);
        }
    }

    private CloudinaryUploadResult doUpload(MultipartFile file, String folder, String resourceType) {
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", resourceType,
                            "unique_filename", true,
                            "overwrite", false
                    ));
            String url = String.valueOf(uploadResult.get("secure_url"));
            String publicId = String.valueOf(uploadResult.get("public_id"));
            log.info("Uploaded file to Cloudinary: folder={}, publicId={}", folder, publicId);
            return new CloudinaryUploadResult(url, publicId);
        } catch (IOException e) {
            log.error("Cloudinary upload failed for folder {}", folder, e);
            throw new BusinessException("IMAGE_UPLOAD_FAILED", "Failed to upload image. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validate(MultipartFile file, Set<String> allowedExtensions, long maxSizeMb, String kind) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(kind + "_FILE_REQUIRED", "A file is required", HttpStatus.BAD_REQUEST);
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (extension == null || !allowedExtensions.contains(extension)) {
            throw new BusinessException(
                    kind + "_INVALID_TYPE",
                    "Unsupported file type. Allowed: " + String.join(", ", allowedExtensions),
                    HttpStatus.BAD_REQUEST);
        }

        long maxBytes = maxSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BusinessException(
                    kind + "_TOO_LARGE",
                    "File exceeds the maximum allowed size of " + maxSizeMb + "MB",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
