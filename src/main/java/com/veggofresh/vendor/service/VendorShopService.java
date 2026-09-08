package com.veggofresh.vendor.service;

import com.veggofresh.auth.service.UserLookupService;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.platform.storage.CloudinaryService;
import com.veggofresh.platform.storage.CloudinaryUploadResult;
import com.veggofresh.vendor.dto.request.ShopUpdateRequestDto;
import com.veggofresh.vendor.dto.request.StoreProfileRequestDto;
import com.veggofresh.vendor.dto.request.VendorAccountSettingsRequestDto;
import com.veggofresh.vendor.dto.response.ShopDto;
import com.veggofresh.vendor.dto.response.StoreProfileResponseDto;
import com.veggofresh.vendor.dto.response.VendorAccountSettingsResponseDto;
import com.veggofresh.vendor.entity.KycStatus;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MODIFIED: registerShop() and submitKycDocuments() removed (see NOTES_VENDOR.md).
 * ADDED: getStoreProfile/updateStoreProfile, getAccountSettings/updateAccountSettings.
 * MODIFIED: storeImageUrl/profileImageUrl are now real Cloudinary uploads (see
 * updateStoreProfile/updateAccountSettings) instead of raw URL strings passed by the client.
 */
@Service
@RequiredArgsConstructor
public class VendorShopService {

    private final ShopRepository shopRepository;
    private final UserLookupService userLookupService;
    private final CloudinaryService cloudinaryService;

    @Transactional(readOnly = true)
    public ShopDto getShopByOwner(UUID ownerUserId) {
        return mapToDto(requireShop(ownerUserId));
    }

    @Transactional
    public ShopDto updateShop(UUID ownerUserId, ShopUpdateRequestDto request) {
        Shop shop = requireShop(ownerUserId);

        shop.setName(request.getName());
        shop.setAddress(request.getAddress());
        shop.setLatitude(request.getLatitude());
        shop.setLongitude(request.getLongitude());

        return mapToDto(shopRepository.save(shop));
    }

    @Transactional
    public void setShopStatus(UUID ownerUserId, boolean isOnline) {
        Shop shop = requireShop(ownerUserId);

        if (isOnline && shop.getKycStatus() != KycStatus.APPROVED) {
            throw new BusinessException("VENDOR_KYC_NOT_APPROVED", "Cannot go online. KYC is not approved.");
        }

        shop.setOnline(isOnline);
        shopRepository.save(shop);
    }

    @Transactional(readOnly = true)
    public StoreProfileResponseDto getStoreProfile(UUID ownerUserId) {
        return mapToStoreProfileDto(requireShop(ownerUserId));
    }

    @Transactional
    public StoreProfileResponseDto updateStoreProfile(UUID ownerUserId, StoreProfileRequestDto request) {
        Shop shop = requireShop(ownerUserId);

        shop.setName(request.getStoreName());
        shop.setStoreBio(request.getStoreBio());
        if (request.getAttributes() != null) {
            shop.setStoreAttributes(String.join(";", request.getAttributes()));
        }
        shop.setStreetAddress(request.getStreetAddress());
        shop.setCity(request.getCity());
        shop.setZipCode(request.getZipCode());
        if (request.getLatitude() != null) shop.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) shop.setLongitude(request.getLongitude());
        // Keep legacy single-string 'address' in sync for existing callers.
        shop.setAddress(request.getStreetAddress() + ", " + request.getCity()
                + (shop.getState() != null ? ", " + shop.getState() : "") + " " + request.getZipCode());

        // Store photo: optional, single. Omitting it leaves the current photo untouched --
        // this does NOT follow the "always overwrite" behavior of the fields above.
        if (request.getStoreImage() != null && !request.getStoreImage().isEmpty()) {
            CloudinaryUploadResult upload = cloudinaryService.uploadImage(
                    request.getStoreImage(), "veggofresh/vendors/" + shop.getId() + "/store");
            String oldPublicId = shop.getStoreImagePublicId();
            shop.setStoreImageUrl(upload.url());
            shop.setStoreImagePublicId(upload.publicId());
            cloudinaryService.deleteQuietly(oldPublicId);
        }

        return mapToStoreProfileDto(shopRepository.save(shop));
    }

    @Transactional(readOnly = true)
    public VendorAccountSettingsResponseDto getAccountSettings(UUID ownerUserId) {
        return mapToAccountSettingsDto(requireShop(ownerUserId));
    }

    @Transactional
    public VendorAccountSettingsResponseDto updateAccountSettings(UUID ownerUserId, VendorAccountSettingsRequestDto request) {
        Shop shop = requireShop(ownerUserId);

        if (request.getFullName() != null) shop.setFullName(request.getFullName());
        if (request.getEmail() != null) shop.setEmail(request.getEmail());
        if (request.getBusinessPhone() != null) shop.setBusinessPhone(request.getBusinessPhone());
        if (request.getBusinessLicenseNumber() != null) shop.setBusinessLicenseNumber(request.getBusinessLicenseNumber());
        if (request.getNewOrderAlertsEnabled() != null) shop.setNewOrderAlertsEnabled(request.getNewOrderAlertsEnabled());
        if (request.getLowStockNotificationsEnabled() != null) shop.setLowStockNotificationsEnabled(request.getLowStockNotificationsEnabled());
        if (request.getPayoutConfirmationsEnabled() != null) shop.setPayoutConfirmationsEnabled(request.getPayoutConfirmationsEnabled());

        // Owner's personal photo: optional, single, patch semantics like every other field here.
        if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
            CloudinaryUploadResult upload = cloudinaryService.uploadImage(
                    request.getProfileImage(), "veggofresh/vendors/" + shop.getId() + "/profile");
            String oldPublicId = shop.getProfileImagePublicId();
            shop.setProfileImageUrl(upload.url());
            shop.setProfileImagePublicId(upload.publicId());
            cloudinaryService.deleteQuietly(oldPublicId);
        }

        return mapToAccountSettingsDto(shopRepository.save(shop));
    }

    private Shop requireShop(UUID ownerUserId) {
        return shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found", HttpStatus.NOT_FOUND));
    }

    private StoreProfileResponseDto mapToStoreProfileDto(Shop shop) {
        List<String> attributes = shop.getStoreAttributes() != null
                ? Arrays.stream(shop.getStoreAttributes().split(";")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList())
                : List.of();

        return StoreProfileResponseDto.builder()
                .id(shop.getId())
                .storeName(shop.getName())
                .storeBio(shop.getStoreBio())
                .storeImageUrl(shop.getStoreImageUrl())
                .attributes(attributes)
                .streetAddress(shop.getStreetAddress())
                .city(shop.getCity())
                .zipCode(shop.getZipCode())
                .latitude(shop.getLatitude())
                .longitude(shop.getLongitude())
                .isOnline(shop.isOnline())
                .build();
    }

    private VendorAccountSettingsResponseDto mapToAccountSettingsDto(Shop shop) {
        return VendorAccountSettingsResponseDto.builder()
                .fullName(shop.getFullName())
                .email(shop.getEmail())
                .businessPhone(shop.getBusinessPhone())
                .businessLicenseNumber(shop.getBusinessLicenseNumber())
                .profileImageUrl(shop.getProfileImageUrl())
                .newOrderAlertsEnabled(shop.isNewOrderAlertsEnabled())
                .lowStockNotificationsEnabled(shop.isLowStockNotificationsEnabled())
                .payoutConfirmationsEnabled(shop.isPayoutConfirmationsEnabled())
                .build();
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
