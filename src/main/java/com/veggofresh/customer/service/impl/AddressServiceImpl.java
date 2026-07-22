package com.veggofresh.customer.service.impl;

import com.veggofresh.customer.dto.request.AddressRequestDto;
import com.veggofresh.customer.dto.response.AddressResponseDto;
import com.veggofresh.customer.entity.Address;
import com.veggofresh.customer.repository.AddressRepository;
import com.veggofresh.customer.service.AddressService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponseDto> getAddresses(UUID userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public AddressResponseDto addAddress(UUID userId, AddressRequestDto request) {
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            resetDefaultAddress(userId);
        }

        Address address = new Address();
        address.setUserId(userId);
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setDefault(request.getIsDefault() != null ? request.getIsDefault() : false);

        Address saved = addressRepository.save(address);
        return mapToDto(saved);
    }

    @Override
    public AddressResponseDto updateAddress(UUID userId, UUID addressId, AddressRequestDto request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BusinessException("ADDRESS_NOT_FOUND", "Address not found or does not belong to user", HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            resetDefaultAddress(userId);
        }

        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setDefault(request.getIsDefault() != null ? request.getIsDefault() : false);

        Address updated = addressRepository.save(address);
        return mapToDto(updated);
    }

    @Override
    public void deleteAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BusinessException("ADDRESS_NOT_FOUND", "Address not found or does not belong to user", HttpStatus.NOT_FOUND));

        address.softDelete();
        addressRepository.save(address);
    }

    private void resetDefaultAddress(UUID userId) {
        List<Address> addresses = addressRepository.findByUserId(userId);
        for (Address addr : addresses) {
            if (addr.isDefault()) {
                addr.setDefault(false);
                addressRepository.save(addr);
            }
        }
    }

    private AddressResponseDto mapToDto(Address address) {
        return AddressResponseDto.builder()
                .id(address.getId())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .isDefault(address.isDefault())
                .build();
    }
}
