package com.veggofresh.auth.service.impl;

import com.veggofresh.auth.dto.UserSummaryDto;
import com.veggofresh.auth.entity.User;
import com.veggofresh.auth.repository.UserRepository;
import com.veggofresh.auth.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserLookupServiceImpl implements UserLookupService {

    private final UserRepository userRepository;

    @Override
    public Optional<UserSummaryDto> findById(UUID userId) {
        return userRepository.findById(userId).map(this::mapToSummary);
    }

    @Override
    public Optional<UserSummaryDto> findByPhone(String phone) {
        return userRepository.findByPhone(phone).map(this::mapToSummary);
    }

    @Override
    public Optional<UserSummaryDto> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::mapToSummary);
    }

    private UserSummaryDto mapToSummary(User user) {
        return UserSummaryDto.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                .isVerified(user.isVerified())
                .isBlocked(user.isBlocked())
                .build();
    }
}
