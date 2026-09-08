package com.veggofresh.platform.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires up the Cloudinary SDK client as a Spring bean for the whole application.
 *
 * <p>This is the single source of Cloudinary credentials for every module — Customer,
 * Vendor, Delivery, and Admin all upload through {@link com.veggofresh.platform.storage.CloudinaryService},
 * which depends on the {@link Cloudinary} bean created here.
 *
 * <h3>Configuration (application.yml)</h3>
 * <pre>
 * veggofresh:
 *   cloudinary:
 *     cloud-name: ${CLOUDINARY_CLOUD_NAME}
 *     api-key: ${CLOUDINARY_API_KEY}
 *     api-secret: ${CLOUDINARY_API_SECRET}
 * </pre>
 *
 * <p><b>No default values are provided for these properties in any committed yml file</b> —
 * exactly like {@code DB_URL}/{@code DB_USERNAME}/{@code DB_PASSWORD}. You must export
 * {@code CLOUDINARY_CLOUD_NAME}, {@code CLOUDINARY_API_KEY}, and {@code CLOUDINARY_API_SECRET}
 * as environment variables (IDE run config, shell env, or your secrets manager in prod)
 * before starting the app, in every profile including local.
 */
@Configuration
public class CloudinaryConfig {

    @Value("${veggofresh.cloudinary.cloud-name}")
    private String cloudName;

    @Value("${veggofresh.cloudinary.api-key}")
    private String apiKey;

    @Value("${veggofresh.cloudinary.api-secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }
}
