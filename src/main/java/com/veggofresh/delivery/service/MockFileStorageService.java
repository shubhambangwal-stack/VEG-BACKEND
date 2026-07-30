package com.veggofresh.delivery.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * MOCK file storage. No real S3/Cloudinary/disk integration exists anywhere in the
 * codebase yet (confirmed with the team before building this). Logs the upload and
 * returns a fake, deterministic URL so the rest of the flow (status transitions,
 * response shape) is fully testable. Swap the body of store() for a real client
 * once one exists -- the method signature shouldn't need to change.
 */
@Slf4j
@Service
public class MockFileStorageService {

    public String store(MultipartFile file, String pathPrefix) {
        String fileName = file != null ? file.getOriginalFilename() : "unknown";
        long size = file != null ? file.getSize() : 0;
        String fakeUrl = "mock://" + pathPrefix + "/" + UUID.randomUUID() + "-" + fileName;

        log.info("MOCK STORAGE — received file '{}' ({} bytes), pretending to store at {}", fileName, size, fakeUrl);
        return fakeUrl;
    }
}
