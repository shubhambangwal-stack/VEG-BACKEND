package com.veggofresh.vendor.dto.response;

import com.veggofresh.payment.dto.UserBankAccountDto;
import com.veggofresh.vendor.entity.KycStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class VendorApplicationDetailsResponseDto {
    private List<VendorDocumentResponseDto> documents;
    private UserBankAccountDto bankDetails;
    
    // Basic Info
    private String fullName;
    private String email;
    private String businessPhone;
    private String businessType;
    
    // Business Location
    private String streetAddress;
    private String city;
    private String state;
    private String zipCode;
    
    private KycStatus kycStatus;
}
