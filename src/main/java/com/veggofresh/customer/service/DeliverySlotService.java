package com.veggofresh.customer.service;

import com.veggofresh.customer.dto.response.DeliverySlotDto;

import java.util.List;

public interface DeliverySlotService {
    /**
     * Gets available delivery slots for the selected date (optional: today/tomorrow slots).
     * If date string is empty or null, returns slots for today and tomorrow.
     */
    List<DeliverySlotDto> getAvailableSlots(String dateStr);
}
