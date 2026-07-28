package com.veggofresh.customer.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PromoCodeRequestDto {
    /** The promo code string to validate and apply */
    private String code;
}
