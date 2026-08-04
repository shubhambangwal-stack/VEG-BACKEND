package com.veggofresh.vendor.service.impl;

import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.request.OperatingHourUpdateRequestDto;
import com.veggofresh.vendor.dto.request.SpecialClosureRequestDto;
import com.veggofresh.vendor.dto.response.OperatingHourResponseDto;
import com.veggofresh.vendor.dto.response.OperatingHoursSummaryResponseDto;
import com.veggofresh.vendor.dto.response.SpecialClosureResponseDto;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.entity.VendorOperatingHour;
import com.veggofresh.vendor.entity.VendorSpecialClosure;
import com.veggofresh.vendor.repository.ShopRepository;
import com.veggofresh.vendor.repository.VendorOperatingHourRepository;
import com.veggofresh.vendor.repository.VendorSpecialClosureRepository;
import com.veggofresh.vendor.service.VendorOperatingHoursService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VendorOperatingHoursServiceImpl implements VendorOperatingHoursService {

    private static final LocalTime DEFAULT_OPEN = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_CLOSE = LocalTime.of(18, 0);
    private static final List<DayOfWeek> DEFAULT_OPEN_DAYS = List.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

    private final ShopRepository shopRepository;
    private final VendorOperatingHourRepository operatingHourRepository;
    private final VendorSpecialClosureRepository closureRepository;

    @Override
    public OperatingHoursSummaryResponseDto getOperatingHours(UUID ownerUserId) {
        Shop shop = requireShop(ownerUserId);
        List<VendorOperatingHour> hours = operatingHourRepository.findByShopId(shop.getId());

        for (DayOfWeek day : DayOfWeek.values()) {
            boolean present = hours.stream().anyMatch(h -> h.getDayOfWeek() == day);
            if (!present) {
                VendorOperatingHour hour = new VendorOperatingHour();
                hour.setShopId(shop.getId());
                hour.setDayOfWeek(day);
                boolean defaultOpen = DEFAULT_OPEN_DAYS.contains(day);
                hour.setOpen(defaultOpen);
                hour.setOpenTime(defaultOpen ? DEFAULT_OPEN : null);
                hour.setCloseTime(defaultOpen ? DEFAULT_CLOSE : null);
                hours.add(operatingHourRepository.save(hour));
            }
        }

        List<VendorSpecialClosure> closures = closureRepository.findByShopId(shop.getId());

        return buildSummary(shop, hours, closures);
    }

    @Override
    public OperatingHoursSummaryResponseDto updateOperatingHours(UUID ownerUserId, List<OperatingHourUpdateRequestDto> updates) {
        Shop shop = requireShop(ownerUserId);

        for (OperatingHourUpdateRequestDto update : updates) {
            if (Boolean.TRUE.equals(update.getIsOpen())) {
                if (update.getOpenTime() == null || update.getCloseTime() == null) {
                    throw new BusinessException("VENDOR_HOURS_TIME_REQUIRED",
                            update.getDayOfWeek() + " is marked open but is missing an open/close time", HttpStatus.BAD_REQUEST);
                }
                if (!update.getCloseTime().isAfter(update.getOpenTime())) {
                    throw new BusinessException("VENDOR_HOURS_INVALID_RANGE",
                            update.getDayOfWeek() + "'s close time must be after its open time", HttpStatus.BAD_REQUEST);
                }
            }

            VendorOperatingHour hour = operatingHourRepository.findByShopIdAndDayOfWeek(shop.getId(), update.getDayOfWeek())
                    .orElseGet(() -> {
                        VendorOperatingHour newHour = new VendorOperatingHour();
                        newHour.setShopId(shop.getId());
                        newHour.setDayOfWeek(update.getDayOfWeek());
                        return newHour;
                    });

            hour.setOpen(update.getIsOpen());
            hour.setOpenTime(update.getIsOpen() ? update.getOpenTime() : null);
            hour.setCloseTime(update.getIsOpen() ? update.getCloseTime() : null);
            operatingHourRepository.save(hour);
        }

        List<VendorOperatingHour> hours = operatingHourRepository.findByShopId(shop.getId());
        List<VendorSpecialClosure> closures = closureRepository.findByShopId(shop.getId());
        return buildSummary(shop, hours, closures);
    }

    @Override
    public SpecialClosureResponseDto addSpecialClosure(UUID ownerUserId, SpecialClosureRequestDto request) {
        Shop shop = requireShop(ownerUserId);

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("VENDOR_CLOSURE_INVALID_RANGE", "End date cannot be before start date", HttpStatus.BAD_REQUEST);
        }

        VendorSpecialClosure closure = new VendorSpecialClosure();
        closure.setShopId(shop.getId());
        closure.setName(request.getName());
        closure.setStartDate(request.getStartDate());
        closure.setEndDate(request.getEndDate());
        closure = closureRepository.save(closure);

        return mapClosureToDto(closure);
    }

    @Override
    public void deleteSpecialClosure(UUID ownerUserId, UUID closureId) {
        Shop shop = requireShop(ownerUserId);

        VendorSpecialClosure closure = closureRepository.findByIdAndShopId(closureId, shop.getId())
                .orElseThrow(() -> new BusinessException("VENDOR_CLOSURE_NOT_FOUND", "Special closure not found", HttpStatus.NOT_FOUND));

        closure.softDelete();
        closureRepository.save(closure);
    }

    private Shop requireShop(UUID ownerUserId) {
        return shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found", HttpStatus.NOT_FOUND));
    }

    private OperatingHoursSummaryResponseDto buildSummary(Shop shop, List<VendorOperatingHour> hours, List<VendorSpecialClosure> closures) {
        List<OperatingHourResponseDto> schedule = hours.stream()
                .sorted((a, b) -> a.getDayOfWeek().compareTo(b.getDayOfWeek()))
                .map(h -> OperatingHourResponseDto.builder()
                        .dayOfWeek(h.getDayOfWeek())
                        .isOpen(h.isOpen())
                        .openTime(h.getOpenTime())
                        .closeTime(h.getCloseTime())
                        .build())
                .collect(Collectors.toList());

        List<SpecialClosureResponseDto> closureDtos = closures.stream()
                .map(this::mapClosureToDto)
                .collect(Collectors.toList());

        return OperatingHoursSummaryResponseDto.builder()
                .storeOnline(shop.isOnline())
                .weeklySchedule(schedule)
                .specialClosures(closureDtos)
                .build();
    }

    private SpecialClosureResponseDto mapClosureToDto(VendorSpecialClosure closure) {
        return SpecialClosureResponseDto.builder()
                .id(closure.getId())
                .name(closure.getName())
                .startDate(closure.getStartDate())
                .endDate(closure.getEndDate())
                .build();
    }
}
