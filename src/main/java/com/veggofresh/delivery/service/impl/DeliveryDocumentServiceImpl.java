package com.veggofresh.delivery.service.impl;

import com.veggofresh.delivery.dto.response.DeliveryDocumentResponseDto;
import com.veggofresh.delivery.entity.DeliveryDocument;
import com.veggofresh.delivery.entity.DeliveryDocumentStatus;
import com.veggofresh.delivery.entity.DeliveryDocumentType;
import com.veggofresh.delivery.repository.DeliveryDocumentRepository;
import com.veggofresh.delivery.repository.DeliveryPartnerProfileRepository;
import com.veggofresh.delivery.service.DeliveryDocumentService;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.platform.storage.CloudinaryService;
import com.veggofresh.platform.storage.CloudinaryUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryDocumentServiceImpl implements DeliveryDocumentService {

    private final DeliveryDocumentRepository documentRepository;
    private final DeliveryPartnerProfileRepository partnerRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    public List<DeliveryDocumentResponseDto> getDocuments(UUID userId) {
        requirePartnerExists(userId);

        List<DeliveryDocument> existing = documentRepository.findByDeliveryPartnerUserId(userId);

        for (DeliveryDocumentType type : DeliveryDocumentType.values()) {
            boolean present = existing.stream().anyMatch(d -> d.getDocumentType() == type);
            if (!present) {
                DeliveryDocument doc = new DeliveryDocument();
                doc.setDeliveryPartnerUserId(userId);
                doc.setDocumentType(type);
                doc.setStatus(DeliveryDocumentStatus.PENDING);
                existing.add(documentRepository.save(doc));
            }
        }

        return existing.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public DeliveryDocumentResponseDto uploadDocument(UUID userId, DeliveryDocumentType type, MultipartFile file, LocalDate expiryDate) {
        requirePartnerExists(userId);

        if (file == null || file.isEmpty()) {
            throw new BusinessException("DELIVERY_DOCUMENT_FILE_REQUIRED", "A file is required", HttpStatus.BAD_REQUEST);
        }

        DeliveryDocument doc = documentRepository.findByDeliveryPartnerUserIdAndDocumentType(userId, type)
                .orElseGet(() -> {
                    DeliveryDocument newDoc = new DeliveryDocument();
                    newDoc.setDeliveryPartnerUserId(userId);
                    newDoc.setDocumentType(type);
                    return newDoc;
                });

        // Upload the new document first -- only swap over and delete the old one (if this
        // document type was previously uploaded) once the new upload has actually succeeded.
        CloudinaryUploadResult upload = cloudinaryService.uploadDocument(
                file, "veggofresh/delivery-documents/" + userId + "/" + type.name());
        String oldPublicId = doc.getPublicId();

        doc.setFileUrl(upload.url());
        doc.setPublicId(upload.publicId());
        // Any (re-)upload resets to PENDING -- real verification is a manual/admin
        // step that doesn't exist yet. Nothing auto-flips this to VERIFIED.
        doc.setStatus(DeliveryDocumentStatus.PENDING);
        if (expiryDate != null) {
            doc.setExpiryDate(expiryDate);
        }

        DeliveryDocumentResponseDto response = mapToDto(documentRepository.save(doc));
        cloudinaryService.deleteQuietly(oldPublicId);
        return response;
    }

    private void requirePartnerExists(UUID userId) {
        partnerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("DELIVERY_PROFILE_NOT_FOUND", "Delivery partner profile not found", HttpStatus.NOT_FOUND));
    }

    private DeliveryDocumentResponseDto mapToDto(DeliveryDocument doc) {
        return DeliveryDocumentResponseDto.builder()
                .documentType(doc.getDocumentType())
                .status(doc.getStatus())
                .fileUrl(doc.getFileUrl())
                .expiryDate(doc.getExpiryDate())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
