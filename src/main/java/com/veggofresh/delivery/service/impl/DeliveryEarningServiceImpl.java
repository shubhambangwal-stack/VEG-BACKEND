package com.veggofresh.delivery.service.impl;

import com.veggofresh.delivery.dto.response.EarningsSummaryResponseDto;
import com.veggofresh.delivery.entity.EarningRecord;
import com.veggofresh.delivery.repository.EarningRecordRepository;
import com.veggofresh.delivery.service.DeliveryEarningService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryEarningServiceImpl implements DeliveryEarningService {

    private final EarningRecordRepository earningRecordRepository;

    @Override
    public EarningsSummaryResponseDto getEarnings(UUID deliveryPartnerUserId, String period) {
        String normalizedPeriod = period == null ? "daily" : period.toLowerCase();

        Instant from = switch (normalizedPeriod) {
            case "daily" -> Instant.now().minus(1, ChronoUnit.DAYS);
            case "weekly" -> Instant.now().minus(7, ChronoUnit.DAYS);
            case "monthly" -> Instant.now().minus(30, ChronoUnit.DAYS);
            default -> throw new BusinessException("DELIVERY_INVALID_PERIOD", "period must be daily, weekly, or monthly", HttpStatus.BAD_REQUEST);
        };

        List<EarningRecord> records = earningRecordRepository
                .findByDeliveryPartnerUserIdAndCreatedAtBetween(deliveryPartnerUserId, from, Instant.now());

        BigDecimal total = records.stream().map(EarningRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return EarningsSummaryResponseDto.builder()
                .period(normalizedPeriod)
                .totalEarnings(total)
                .totalDeliveries(records.size())
                .entries(records.stream()
                        .map(r -> EarningsSummaryResponseDto.EarningEntryDto.builder()
                                .orderId(r.getOrderId())
                                .amount(r.getAmount())
                                .earnedAt(r.getCreatedAt())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
