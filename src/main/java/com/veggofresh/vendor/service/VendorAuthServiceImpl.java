package com.veggofresh.vendor.service;

import com.veggofresh.auth.entity.RefreshToken;
import com.veggofresh.auth.entity.User;
import com.veggofresh.auth.entity.UserRole;
import com.veggofresh.auth.repository.RefreshTokenRepository;
import com.veggofresh.auth.repository.UserRepository;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.platform.security.JwtTokenProvider;
import com.veggofresh.vendor.dto.request.VendorLoginRequestDto;
import com.veggofresh.vendor.dto.request.VendorRegisterRequestDto;
import com.veggofresh.vendor.dto.response.VendorAuthResponseDto;
import com.veggofresh.vendor.dto.response.VendorProfileDto;
import com.veggofresh.vendor.entity.KycStatus;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VendorAuthServiceImpl implements VendorAuthService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public VendorAuthResponseDto login(VendorLoginRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        if (user.getRole() != UserRole.VENDOR) {
            throw new BusinessException("ACCESS_DENIED", "User is not a vendor", HttpStatus.FORBIDDEN);
        }

        if (user.isBlocked()) {
            throw new BusinessException("USER_BLOCKED", "Vendor account is blocked", HttpStatus.FORBIDDEN);
        }

        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public VendorAuthResponseDto register(VendorRegisterRequestDto request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("EMAIL_EXISTS", "Email is already registered", HttpStatus.CONFLICT);
        }

        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new BusinessException("PHONE_EXISTS", "Phone number is already registered", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(UserRole.VENDOR);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setVerified(true); // Assuming auto-verified for this flow, or require email verification later
        user = userRepository.save(user);

        Shop shop = Shop.builder()
                .ownerUserId(user.getId())
                .name(request.getBusinessName())
                .kycStatus(KycStatus.PENDING)
                .isOnline(false)
                .build();
        shopRepository.save(shop);

        return generateAuthResponse(user);
    }

    private VendorAuthResponseDto generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail(), user.getRole().name());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setToken(refreshTokenStr);
        refreshToken.setExpiresAt(jwtTokenProvider.extractExpiry(refreshTokenStr));
        refreshTokenRepository.save(refreshToken);

        Optional<Shop> shopOpt = shopRepository.findByOwnerUserIdAndDeletedAtIsNull(user.getId());

        VendorProfileDto profileDto = VendorProfileDto.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getEmail()) // Fallback
                .businessName(shopOpt.map(Shop::getName).orElse(""))
                .businessType("General") // To be added to shop entity if required
                .build();

        return VendorAuthResponseDto.builder()
                .token(accessToken)
                .vendor(profileDto)
                .build();
    }
}
