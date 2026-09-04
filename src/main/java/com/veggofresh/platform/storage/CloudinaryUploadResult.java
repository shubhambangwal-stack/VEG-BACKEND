package com.veggofresh.platform.storage;

/**
 * Result of a successful upload to Cloudinary.
 *
 * <p>{@code url} is the public, browsable {@code secure_url} to store on the entity and
 * return to clients. {@code publicId} is Cloudinary's internal asset identifier — it is
 * NOT shown to clients, but must be persisted alongside the URL (e.g. in a sibling
 * {@code *_public_id} column) so the asset can be deleted later via
 * {@link CloudinaryService#delete(String)} when the image is replaced or removed.
 *
 * @param url      the public HTTPS URL of the uploaded asset
 * @param publicId Cloudinary's public_id, required for deletion
 */
public record CloudinaryUploadResult(String url, String publicId) {
}
