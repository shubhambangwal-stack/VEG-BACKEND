package com.veggofresh.delivery.service.impl;

import com.veggofresh.delivery.service.DeliveryAssignmentService;
import com.veggofresh.delivery.service.DeliveryDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryDispatchServiceImpl implements DeliveryDispatchService {

    private final DeliveryAssignmentService deliveryAssignmentService;

    @Override
    public void dispatchOrder(UUID orderId, double pickupLat, double pickupLng, double dropLat, double dropLng) {
        deliveryAssignmentService.createAssignmentForOrder(orderId, pickupLat, pickupLng, dropLat, dropLng);
    }
}
