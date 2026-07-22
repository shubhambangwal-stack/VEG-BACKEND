package com.veggofresh.vendor.service;

import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.entity.KycStatus;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.repository.ShopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorShopServiceTest {

    @Mock
    private ShopRepository shopRepository;

    @InjectMocks
    private VendorShopService vendorShopService;

    private UUID ownerUserId;
    private Shop shop;

    @BeforeEach
    void setUp() {
        ownerUserId = UUID.randomUUID();
        shop = Shop.builder()
                .ownerUserId(ownerUserId)
                .name("Test Shop")
                .kycStatus(KycStatus.PENDING)
                .isOnline(false)
                .build();
    }

    @Test
    void submitKycDocuments_transitionsToApproved() {
        when(shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId))
                .thenReturn(Optional.of(shop));

        vendorShopService.submitKycDocuments(ownerUserId);

        ArgumentCaptor<Shop> captor = ArgumentCaptor.forClass(Shop.class);
        verify(shopRepository).save(captor.capture());

        assertEquals(KycStatus.APPROVED, captor.getValue().getKycStatus());
    }

    @Test
    void setShopStatus_success_whenKycApproved() {
        shop.setKycStatus(KycStatus.APPROVED);
        when(shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId))
                .thenReturn(Optional.of(shop));

        vendorShopService.setShopStatus(ownerUserId, true);

        ArgumentCaptor<Shop> captor = ArgumentCaptor.forClass(Shop.class);
        verify(shopRepository).save(captor.capture());

        assertEquals(true, captor.getValue().isOnline());
    }

    @Test
    void setShopStatus_fails_whenKycNotApproved() {
        when(shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId))
                .thenReturn(Optional.of(shop));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            vendorShopService.setShopStatus(ownerUserId, true);
        });

        assertEquals("VENDOR_KYC_NOT_APPROVED", exception.getErrorCode());
    }
}
