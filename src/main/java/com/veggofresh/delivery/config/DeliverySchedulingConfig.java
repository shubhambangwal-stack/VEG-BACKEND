package com.veggofresh.delivery.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's @Scheduled support for the periodic delivery-assignment expiry
 * sweep (DeliveryAssignmentServiceImpl.scheduledExpirySweep). Kept local to this
 * module so Platform's frozen main application class doesn't need to be touched.
 * If another module already enables scheduling globally, this becomes redundant
 * but harmless — Spring tolerates multiple @EnableScheduling declarations.
 */
@Configuration
@EnableScheduling
public class DeliverySchedulingConfig {
}
