package com.veggofresh.auth.repository;

import com.veggofresh.auth.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {
    Optional<OtpVerification> findTopByPhoneOrderByCreatedAtDesc(String phone);
    List<OtpVerification> findByPhoneAndCreatedAtAfter(String phone, Instant time);
}
