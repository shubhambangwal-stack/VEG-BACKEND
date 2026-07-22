package com.veggofresh.customer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class CustomerOrderServiceImpl implements CustomerOrderService {

    @Override
    public void acceptOrder(UUID orderId) {
        log.info("Mocking customerOrderService.acceptOrder for order {}", orderId);
    }

    @Override
    public void rejectOrder(UUID orderId) {
        log.info("Mocking customerOrderService.rejectOrder for order {}", orderId);
    }

    @Override
    public void updateOrderStatus(UUID orderId, String status) {
        log.info("Mocking customerOrderService.updateOrderStatus for order {} to {}", orderId, status);
    }
}
