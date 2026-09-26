package com.veggofresh.delivery.dto.response;

import com.veggofresh.delivery.entity.DeliveryKycStatus;
import com.veggofresh.payment.dto.UserBankAccountDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DeliveryApplicationDetailsResponseDto {
    private List<DeliveryDocumentResponseDto> documents;
    private UserBankAccountDto bankDetails;
    private String licenseNumber;
    private String vehiclePlateNumber;
    private String vehicleModel;
    private Integer vehicleManufactureYear;
    private DeliveryKycStatus kycStatus;
}
