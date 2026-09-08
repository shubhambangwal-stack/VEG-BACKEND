package com.veggofresh.vendor.service.impl;

import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.platform.storage.CloudinaryService;
import com.veggofresh.platform.storage.CloudinaryUploadResult;
import com.veggofresh.vendor.dto.response.VendorDocumentResponseDto;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.entity.VendorDocument;
import com.veggofresh.vendor.entity.VendorDocumentStatus;
import com.veggofresh.vendor.entity.VendorDocumentType;
import com.veggofresh.vendor.repository.ShopRepository;
import com.veggofresh.vendor.repository.VendorDocumentRepository;
import com.veggofresh.vendor.service.VendorDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VendorDocumentServiceImpl implements VendorDocumentService {

    private final VendorDocumentRepository documentRepository;
    private final ShopRepository shopRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    public List<VendorDocumentResponseDto> getDocuments(UUID ownerUserId) {
        Shop shop = requireShop(ownerUserId);
        List<VendorDocument> existing = documentRepository.findByShopId(shop.getId());

        for (VendorDocumentType type : VendorDocumentType.values()) {
            boolean present = existing.stream().anyMatch(d -> d.getDocumentType() == type);
            if (!present) {
                VendorDocument doc = new VendorDocument();
                doc.setShopId(shop.getId());
                doc.setDocumentType(type);
                doc.setStatus(VendorDocumentStatus.PENDING);
                existing.add(documentRepository.save(doc));
            }
        }

        return existing.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public VendorDocumentResponseDto uploadDocument(UUID ownerUserId, VendorDocumentType type, MultipartFile file) {
        Shop shop = requireShop(ownerUserId);

        if (file == null || file.isEmpty()) {
            throw new BusinessException("VENDOR_DOCUMENT_FILE_REQUIRED", "A file is required", HttpStatus.BAD_REQUEST);
        }

        VendorDocument doc = documentRepository.findByShopIdAndDocumentType(shop.getId(), type)
                .orElseGet(() -> {
                    VendorDocument newDoc = new VendorDocument();
                    newDoc.setShopId(shop.getId());
                    newDoc.setDocumentType(type);
                    return newDoc;
                });

        // Upload the new document first -- only swap over and delete the old one (if this
        // document type was previously uploaded) once the new upload has actually succeeded.
        CloudinaryUploadResult upload = cloudinaryService.uploadDocument(
                file, "veggofresh/vendor-documents/" + shop.getId() + "/" + type.name());
        String oldPublicId = doc.getPublicId();

        doc.setFileUrl(upload.url());
        doc.setPublicId(upload.publicId());
        // Any (re-)upload resets to PENDING -- real verification is a manual/admin
        // step that doesn't exist yet.
        doc.setStatus(VendorDocumentStatus.PENDING);

        VendorDocumentResponseDto response = mapToDto(documentRepository.save(doc));
        cloudinaryService.deleteQuietly(oldPublicId);
        return response;
    }

    private Shop requireShop(UUID ownerUserId) {
        return shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Vendor shop not found", HttpStatus.NOT_FOUND));
    }

    private VendorDocumentResponseDto mapToDto(VendorDocument doc) {
        return VendorDocumentResponseDto.builder()
                .documentType(doc.getDocumentType())
                .status(doc.getStatus())
                .fileUrl(doc.getFileUrl())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
