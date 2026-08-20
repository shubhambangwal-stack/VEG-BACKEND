package com.veggofresh.vendor.service.impl;

import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.VendorKycReviewDto;
import com.veggofresh.vendor.entity.KycStatus;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.repository.ShopRepository;
import com.veggofresh.vendor.service.VendorKycService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Real implementation -- resolves the blocking dependency Admin's
 * AdminVendorKycController was built against. See VendorKycService javadoc and
 * NOTES_VENDOR.md for the full contract this fulfills.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class VendorKycServiceImpl implements VendorKycService {

    private final ShopRepository shopRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<VendorKycReviewDto> listPendingKyc(Pageable pageable) {
        return shopRepository.findByKycStatusAndApplicationSubmittedAtIsNotNull(KycStatus.PENDING, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public VendorKycReviewDto getKycDetail(UUID shopId) {
        Shop shop = getShop(shopId);
        return mapToDto(shop);
    }

    @Override
    public void approveKyc(UUID shopId) {
        Shop shop = getShop(shopId);
        shop.setKycStatus(KycStatus.APPROVED);
        shop.setKycRejectionReason(null);
        shopRepository.save(shop);
    }

    @Override
    public void rejectKyc(UUID shopId, String reason) {
        Shop shop = getShop(shopId);
        shop.setKycStatus(KycStatus.REJECTED);
        shop.setKycRejectionReason(reason);
        shopRepository.save(shop);
    }

    private Shop getShop(UUID shopId) {
        return shopRepository.findByIdAndDeletedAtIsNull(shopId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found", HttpStatus.NOT_FOUND));
    }

    private VendorKycReviewDto mapToDto(Shop shop) {
        return VendorKycReviewDto.builder()
                .shopId(shop.getId())
                .ownerUserId(shop.getOwnerUserId())
                .businessName(shop.getName())
                .ownerFullName(shop.getFullName())
                .businessPhone(shop.getBusinessPhone())
                .email(shop.getEmail())
                .businessType(shop.getBusinessType())
                .kycStatus(shop.getKycStatus().name())
                .rejectionReason(shop.getKycRejectionReason())
                .submittedAt(shop.getApplicationSubmittedAt())
                .build();
    }
}
