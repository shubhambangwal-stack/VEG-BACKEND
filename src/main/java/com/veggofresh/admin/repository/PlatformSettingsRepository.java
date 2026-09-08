package com.veggofresh.admin.repository;

import com.veggofresh.admin.entity.PlatformSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PlatformSettingsRepository extends JpaRepository<PlatformSettings, UUID> {
    // Single-row table -- callers use findAll().stream().findFirst(), same
    // getOrCreate pattern as CustomerProfile, rather than a magic fixed id.
}
