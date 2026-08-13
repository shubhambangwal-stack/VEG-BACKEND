package com.veggofresh.customer.service;

import com.veggofresh.customer.dto.response.DeliverySlotDto;

import java.util.List;

public interface DeliverySlotService {
    List<DeliverySlotDto> getAvailableSlots(String dateStr);
}
