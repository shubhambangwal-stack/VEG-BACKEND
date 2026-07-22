package com.veggofresh.customer.service;

import com.veggofresh.customer.dto.request.AddressRequestDto;
import com.veggofresh.customer.dto.response.AddressResponseDto;

import java.util.List;
import java.util.UUID;

public interface AddressService {
    List<AddressResponseDto> getAddresses(UUID userId);
    AddressResponseDto addAddress(UUID userId, AddressRequestDto request);
    AddressResponseDto updateAddress(UUID userId, UUID addressId, AddressRequestDto request);
    void deleteAddress(UUID userId, UUID addressId);
}
