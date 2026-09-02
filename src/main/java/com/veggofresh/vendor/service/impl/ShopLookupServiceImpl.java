package com.veggofresh.vendor.service.impl;

import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.repository.ShopRepository;
import com.veggofresh.vendor.service.ShopLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Straightforward shop-id → owner mapping. Reads only {@code ownerUserId}; no
 * cross-module entity leakage.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopLookupServiceImpl implements ShopLookupService {

    private final ShopRepository shopRepository;

    @Override
    public Optional<UUID> findOwnerUserIdByShopId(UUID shopId) {
        return shopRepository.findByIdAndDeletedAtIsNull(shopId)
                .map(Shop::getOwnerUserId);
    }
}