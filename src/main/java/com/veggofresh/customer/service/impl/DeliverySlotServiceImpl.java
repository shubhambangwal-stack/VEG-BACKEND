package com.veggofresh.customer.service.impl;

import com.veggofresh.customer.dto.response.DeliverySlotDto;
import com.veggofresh.customer.entity.DeliverySlot;
import com.veggofresh.customer.repository.DeliverySlotRepository;
import com.veggofresh.customer.service.DeliverySlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliverySlotServiceImpl implements DeliverySlotService {

    private final DeliverySlotRepository deliverySlotRepository;

    @Override
    public List<DeliverySlotDto> getAvailableSlots(String dateStr) {
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            LocalDate targetDate = LocalDate.parse(dateStr.trim());
            return deliverySlotRepository.findByDateOrderByStartTimeAsc(targetDate).stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }

        // Return slots for both today and tomorrow
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        return deliverySlotRepository.findByDateInOrderByDateAscStartTimeAsc(List.of(today, tomorrow)).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private DeliverySlotDto mapToDto(DeliverySlot slot) {
        return DeliverySlotDto.builder()
                .slotId(slot.getId())
                .date(slot.getDate().toString())
                .label(slot.getLabel())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .isAvailable(slot.isAvailable())
                .build();
    }
}
