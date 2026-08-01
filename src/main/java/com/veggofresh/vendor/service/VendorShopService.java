package com.veggofresh.vendor.service;

import com.veggofresh.auth.service.UserLookupService;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.request.ShopUpdateRequestDto;
import com.veggofresh.vendor.dto.response.ShopDto;
import com.veggofresh.vendor.entity.KycStatus;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.repository.ShopRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * MODIFIED: registerShop() and submitKycDocuments() removed.
 * - Shop creation: now implicit via VendorOnboardingServiceImpl.getOrCreate() on the
 *   first onboarding call, same pattern as Delivery's DeliveryPartnerProfile.
 * - KYC submission: now goes through VendorOnboardingServiceImpl.submitApplication(),
 *   which actually requires documents to be uploaded first. The old
 *   submitKycDocuments() here auto-approved KYC synchronously with zero checks --
 *   that was a real bug, not a placeholder; the auto-approve behavior now only
 *   exists in VendorTestController, clearly marked as fake. See NOTES_VENDOR.md.
 */
@Service
@RequiredArgsConstructor
public class VendorShopService {

    private final ShopRepository shopRepository;
    private final UserLookupService userLookupService;

    @Transactional(readOnly = true)
    public ShopDto getShopByOwner(UUID ownerUserId) {
        Shop shop = shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found"));
        return mapToDto(shop);
    }

    @Transactional
    public ShopDto updateShop(UUID ownerUserId, ShopUpdateRequestDto request) {
        Shop shop = shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found"));

        shop.setName(request.getName());
        shop.setAddress(request.getAddress());
        shop.setLatitude(request.getLatitude());
        shop.setLongitude(request.getLongitude());

        return mapToDto(shopRepository.save(shop));
    }

    @Transactional
    public void setShopStatus(UUID ownerUserId, boolean isOnline) {
        Shop shop = shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found"));

        if (isOnline && shop.getKycStatus() != KycStatus.APPROVED) {
            throw new BusinessException("VENDOR_KYC_NOT_APPROVED", "Cannot go online. KYC is not approved.");
        }

        shop.setOnline(isOnline);
        shopRepository.save(shop);
    }

    public ShopDto mapToDto(Shop shop) {
        return ShopDto.builder()
                .id(shop.getId())
                .ownerUserId(shop.getOwnerUserId())
                .name(shop.getName())
                .address(shop.getAddress())
                .latitude(shop.getLatitude())
                .longitude(shop.getLongitude())
                .kycStatus(shop.getKycStatus().name())
                .isOnline(shop.isOnline())
                .build();
    }
}
