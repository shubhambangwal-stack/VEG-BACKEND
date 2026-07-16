package com.veggofresh.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * VegGo Fresh Platform — Main Application Entry Point.
 *
 * <p>This is the Phase 0 platform foundation. No business modules are loaded here.
 * Each feature module (auth, customer, vendor, delivery, admin, payment, notification)
 * lives in its own sub-package and is independently bootstrapped via component scanning.
 *
 * <p>Module boundary rule: modules must NOT import each other's entity classes directly.
 * Cross-module calls must go through {@code @Service} interfaces only.
 */
@SpringBootApplication(scanBasePackages = "com.veggofresh")
@EnableJpaRepositories(basePackages = "com.veggofresh")
@EntityScan(basePackages = "com.veggofresh")
@EnableJpaAuditing
public class VeggofreshApplication {
    public static void main(String[] args) {
        SpringApplication.run(VeggofreshApplication.class, args);
    }
}
