package com.veggofresh.delivery.service.impl;

import com.veggofresh.delivery.dto.response.EarningsSummaryResponseDto;
import com.veggofresh.delivery.dto.response.EarningsTrendResponseDto;
import com.veggofresh.delivery.entity.DeliveryOnlineSession;
import com.veggofresh.delivery.entity.EarningRecord;
import com.veggofresh.delivery.repository.DeliveryOnlineSessionRepository;
import com.veggofresh.delivery.repository.EarningRecordRepository;
import com.veggofresh.delivery.service.DeliveryEarningService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryEarningServiceImpl implements DeliveryEarningService {

    private final EarningRecordRepository earningRecordRepository;
    private final DeliveryOnlineSessionRepository sessionRepository;

    @Override
    public EarningsSummaryResponseDto getEarnings(UUID deliveryPartnerUserId, String period) {
        String normalizedPeriod = period == null ? "daily" : period.toLowerCase();

        Instant from = switch (normalizedPeriod) {
            case "daily" -> Instant.now().minus(1, ChronoUnit.DAYS);
            case "weekly" -> Instant.now().minus(7, ChronoUnit.DAYS);
            case "monthly" -> Instant.now().minus(30, ChronoUnit.DAYS);
            default -> throw new BusinessException("DELIVERY_INVALID_PERIOD", "period must be daily, weekly, or monthly", HttpStatus.BAD_REQUEST);
        };
        Instant to = Instant.now();

        List<EarningRecord> records = earningRecordRepository
                .findByDeliveryPartnerUserIdAndCreatedAtBetween(deliveryPartnerUserId, from, to);

        BigDecimal total = records.stream().map(EarningRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long activeMinutes = calculateActiveMinutes(deliveryPartnerUserId, from, to);

        return EarningsSummaryResponseDto.builder()
                .period(normalizedPeriod)
                .totalEarnings(total)
                .totalDeliveries(records.size())
                .totalActiveMinutes(activeMinutes)
                .entries(records.stream().map(this::mapToEntryDto).collect(Collectors.toList()))
                .build();
    }

    @Override
    public EarningsTrendResponseDto getTrend(UUID deliveryPartnerUserId, int days) {
        int windowDays = days > 0 ? days : 7;
        Instant from = Instant.now().minus(windowDays, ChronoUnit.DAYS);

        List<EarningRecord> records = earningRecordRepository
                .findByDeliveryPartnerUserIdAndCreatedAtBetween(deliveryPartnerUserId, from, Instant.now());

        List<EarningsTrendResponseDto.DailyEarningDto> days_ = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (int i = windowDays - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now(ZoneOffset.UTC).minusDays(i);
            List<EarningRecord> dayRecords = records.stream()
                    .filter(r -> r.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().equals(date))
                    .collect(Collectors.toList());
            BigDecimal dayTotal = dayRecords.stream().map(EarningRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            grandTotal = grandTotal.add(dayTotal);

            days_.add(EarningsTrendResponseDto.DailyEarningDto.builder()
                    .date(date)
                    .total(dayTotal)
                    .deliveryCount(dayRecords.size())
                    .build());
        }

        return EarningsTrendResponseDto.builder()
                .totalForPeriod(grandTotal)
                .days(days_)
                .build();
    }

    /** Clips each session to [from, to] and sums the overlap. Still-open sessions count up to 'to'. */
    private long calculateActiveMinutes(UUID deliveryPartnerUserId, Instant from, Instant to) {
        List<DeliveryOnlineSession> sessions = sessionRepository
                .findByDeliveryPartnerUserIdAndStartedAtBefore(deliveryPartnerUserId, to);

        long totalSeconds = 0;
        for (DeliveryOnlineSession session : sessions) {
            Instant sessionEnd = session.getEndedAt() != null ? session.getEndedAt() : to;
            Instant clippedStart = session.getStartedAt().isBefore(from) ? from : session.getStartedAt();
            Instant clippedEnd = sessionEnd.isAfter(to) ? to : sessionEnd;
            if (clippedEnd.isAfter(clippedStart)) {
                totalSeconds += Duration.between(clippedStart, clippedEnd).getSeconds();
            }
        }
        return totalSeconds / 60;
    }

    private EarningsSummaryResponseDto.EarningEntryDto mapToEntryDto(EarningRecord r) {
        return EarningsSummaryResponseDto.EarningEntryDto.builder()
                .orderId(r.getOrderId())
                .basePay(r.getBasePay())
                .distanceFare(r.getDistanceFare())
                .peakBonus(r.getPeakBonus())
                .tip(r.getTip())
                .totalAmount(r.getAmount())
                .earnedAt(r.getCreatedAt())
                .build();
    }
}
