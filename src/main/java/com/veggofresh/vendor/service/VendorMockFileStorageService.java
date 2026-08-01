package com.veggofresh.vendor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * MOCK file storage -- local copy of the same pattern used in Delivery module.
 * Kept as a separate class rather than importing Delivery's version to keep
 * modules fully self-contained; this is a stateless utility, not shared data,
 * so the small duplication is preferable to a cross-module dependency for it.
 * No real S3/Cloudinary/disk integration exists anywhere in the codebase.
 */
@Slf4j
@Service
public class VendorMockFileStorageService {

    public String store(MultipartFile file, String pathPrefix) {
        String fileName = file != null ? file.getOriginalFilename() : "unknown";
        long size = file != null ? file.getSize() : 0;
        String fakeUrl = "mock://" + pathPrefix + "/" + UUID.randomUUID() + "-" + fileName;

        log.info("MOCK STORAGE — received file '{}' ({} bytes), pretending to store at {}", fileName, size, fakeUrl);
        return fakeUrl;
    }
}
