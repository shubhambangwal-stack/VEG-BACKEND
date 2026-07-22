package com.veggofresh.vendor.service;

import com.veggofresh.auth.service.UserLookupService;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.request.ShopRegistrationRequestDto;
import com.veggofresh.vendor.dto.request.ShopUpdateRequestDto;
import com.veggofresh.vendor.dto.response.ShopDto;
import com.veggofresh.vendor.entity.KycStatus;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VendorShopService {

    private final ShopRepository shopRepository;
    private final UserLookupService userLookupService;

    @Transactional
    public ShopDto registerShop(ShopRegistrationRequestDto request) {
        if (shopRepository.findByOwnerUserIdAndDeletedAtIsNull(request.getOwnerUserId()).isPresent()) {
            throw new BusinessException("VENDOR_SHOP_EXISTS", "Vendor already has a registered shop");
        }
        
        userLookupService.findById(request.getOwnerUserId())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));

        Shop shop = Shop.builder()
                .ownerUserId(request.getOwnerUserId())
                .name(request.getName())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .kycStatus(KycStatus.PENDING)
                .isOnline(false)
                .build();

        shop = shopRepository.save(shop);
        return mapToDto(shop);
    }

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
    public void submitKycDocuments(UUID ownerUserId) {
        // Mock KYC document submission logic
        Shop shop = shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found"));
        
        // For demonstration, immediately move to APPROVED. Real logic might just attach documents.
        shop.setKycStatus(KycStatus.APPROVED);
        shopRepository.save(shop);
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
