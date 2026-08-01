package com.veggofresh.vendor.service.impl;

import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.request.VendorBasicInfoRequestDto;
import com.veggofresh.vendor.dto.request.VendorBusinessLocationRequestDto;
import com.veggofresh.vendor.dto.response.VendorOnboardingChecklistResponseDto;
import com.veggofresh.vendor.dto.response.VendorOnboardingNextAction;
import com.veggofresh.vendor.dto.response.VendorOnboardingStatusResponseDto;
import com.veggofresh.vendor.entity.KycStatus;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.entity.VendorDocumentType;
import com.veggofresh.vendor.repository.ProductRepository;
import com.veggofresh.vendor.repository.ShopRepository;
import com.veggofresh.vendor.repository.VendorDocumentRepository;
import com.veggofresh.vendor.service.VendorOnboardingService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VendorOnboardingServiceImpl implements VendorOnboardingService {

    private final ShopRepository shopRepository;
    private final VendorDocumentRepository documentRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public VendorOnboardingStatusResponseDto getStatus(UUID ownerUserId) {
        return mapToStatusDto(getOrCreate(ownerUserId));
    }

    @Override
    public VendorOnboardingStatusResponseDto submitBasicInfo(UUID ownerUserId, VendorBasicInfoRequestDto request) {
        Shop shop = getOrCreate(ownerUserId);

        shop.setFullName(request.getFullName());
        shop.setName(request.getBusinessName());
        shop.setEmail(request.getEmail());
        shop.setBusinessPhone(request.getBusinessPhone());
        shop.setBusinessType(request.getBusinessType());
        shop.setHasBasicInfo(true);
        shopRepository.save(shop);

        return mapToStatusDto(shop);
    }

    @Override
    public VendorOnboardingStatusResponseDto submitBusinessLocation(UUID ownerUserId, VendorBusinessLocationRequestDto request) {
        Shop shop = getOrCreate(ownerUserId);
        requireBasicInfoDone(shop);

        shop.setStreetAddress(request.getStreetAddress());
        shop.setCity(request.getCity());
        shop.setState(request.getState());
        shop.setZipCode(request.getZipCode());
        shop.setLatitude(request.getLatitude());
        shop.setLongitude(request.getLongitude());
        // Keep legacy single-string 'address' in sync for existing callers (ProductCatalogService, ShopDto).
        shop.setAddress(request.getStreetAddress() + ", " + request.getCity() + ", "
                + request.getState() + " " + request.getZipCode());
        shop.setHasBusinessLocation(true);
        shopRepository.save(shop);

        return mapToStatusDto(shop);
    }

    @Override
    public VendorOnboardingStatusResponseDto submitApplication(UUID ownerUserId) {
        Shop shop = getOrCreate(ownerUserId);
        requireBusinessLocationDone(shop);

        List<VendorDocumentType> missing = List.of(VendorDocumentType.values()).stream()
                .filter(type -> documentRepository.findByShopIdAndDocumentType(shop.getId(), type)
                        .map(doc -> doc.getFileUrl() == null)
                        .orElse(true))
                .toList();

        if (!missing.isEmpty()) {
            throw new BusinessException("VENDOR_DOCUMENTS_INCOMPLETE",
                    "Upload all required documents before submitting: " + missing, HttpStatus.BAD_REQUEST);
        }

        shop.setApplicationSubmittedAt(Instant.now());
        // Submitting (re-)enters the review queue -- explicitly PENDING so a
        // resubmission after REJECTED correctly clears the old decision.
        shop.setKycStatus(KycStatus.PENDING);
        shop.setKycRejectionReason(null);
        shopRepository.save(shop);

        return mapToStatusDto(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public VendorOnboardingChecklistResponseDto getChecklist(UUID ownerUserId) {
        Shop shop = shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Vendor shop not found", HttpStatus.NOT_FOUND));

        if (shop.getKycStatus() != KycStatus.APPROVED) {
            throw new BusinessException("VENDOR_NOT_APPROVED", "Checklist is only available once your application is approved", HttpStatus.FORBIDDEN);
        }

        boolean hasFirstProduct = !productRepository.findAllByShopIdAndDeletedAtIsNull(shop.getId()).isEmpty();
        boolean hasDeliveryRange = shop.getDeliveryRangeKm() != null;
        boolean hasPaymentSettings = shop.isPaymentSettingsConfigured();

        return VendorOnboardingChecklistResponseDto.builder()
                .hasFirstProduct(hasFirstProduct)
                .hasDeliveryRange(hasDeliveryRange)
                .hasPaymentSettings(hasPaymentSettings)
                .allComplete(hasFirstProduct && hasDeliveryRange && hasPaymentSettings)
                .build();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Shop getOrCreate(UUID ownerUserId) {
        return shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseGet(() -> {
                    Shop newShop = Shop.builder()
                            .ownerUserId(ownerUserId)
                            .name("Unnamed Shop") // placeholder until basic-info step names it
                            .build();
                    return shopRepository.save(newShop);
                });
    }

    private void requireBasicInfoDone(Shop shop) {
        if (!shop.isHasBasicInfo()) {
            throw new BusinessException("VENDOR_ONBOARDING_BASIC_INFO_REQUIRED", "Submit basic info before business location", HttpStatus.BAD_REQUEST);
        }
    }

    private void requireBusinessLocationDone(Shop shop) {
        if (!shop.isHasBusinessLocation()) {
            throw new BusinessException("VENDOR_ONBOARDING_LOCATION_REQUIRED", "Submit business location before documents", HttpStatus.BAD_REQUEST);
        }
    }

    private VendorOnboardingStatusResponseDto mapToStatusDto(Shop shop) {
        boolean documentsSubmitted = shop.getApplicationSubmittedAt() != null;

        VendorOnboardingNextAction nextAction;
        if (shop.getKycStatus() == KycStatus.REJECTED) {
            nextAction = VendorOnboardingNextAction.REJECTED;
        } else if (!shop.isHasBasicInfo()) {
            nextAction = VendorOnboardingNextAction.BASIC_INFO;
        } else if (!shop.isHasBusinessLocation()) {
            nextAction = VendorOnboardingNextAction.BUSINESS_LOCATION;
        } else if (!documentsSubmitted) {
            nextAction = VendorOnboardingNextAction.VERIFICATION_DOCUMENTS;
        } else if (shop.getKycStatus() == KycStatus.APPROVED) {
            nextAction = VendorOnboardingNextAction.DASHBOARD;
        } else {
            nextAction = VendorOnboardingNextAction.UNDER_REVIEW;
        }

        return VendorOnboardingStatusResponseDto.builder()
                .hasBasicInfo(shop.isHasBasicInfo())
                .hasBusinessLocation(shop.isHasBusinessLocation())
                .documentsSubmitted(documentsSubmitted)
                .kycStatus(shop.getKycStatus())
                .rejectionReason(shop.getKycRejectionReason())
                .applicationSubmittedAt(shop.getApplicationSubmittedAt())
                .nextAction(nextAction)
                .build();
    }
}
