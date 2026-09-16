package com.veggofresh.notification.repository;

import com.veggofresh.notification.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, UUID> {

    List<UserDeviceToken> findByUserId(UUID userId);

    @Query("SELECT t FROM UserDeviceToken t WHERE t.userId IN :userIds")
    List<UserDeviceToken> findByUserIds(@Param("userIds") List<UUID> userIds);

    Optional<UserDeviceToken> findByFcmToken(String fcmToken);

    Optional<UserDeviceToken> findByUserIdAndFcmToken(UUID userId, String fcmToken);

    @Modifying
    @Query("DELETE FROM UserDeviceToken t WHERE t.fcmToken = :fcmToken")
    void deleteByFcmToken(@Param("fcmToken") String fcmToken);

    @Modifying
    @Query("DELETE FROM UserDeviceToken t WHERE t.userId = :userId AND t.fcmToken = :fcmToken")
    void deleteByUserIdAndFcmToken(@Param("userId") UUID userId, @Param("fcmToken") String fcmToken);
}
