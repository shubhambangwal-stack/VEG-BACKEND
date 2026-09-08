package com.veggofresh.notification.repository;

import com.veggofresh.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Newest-first inbox for one recipient (the {@code @Where} filter drops soft-deleted rows). */
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    /** Badge count: how many unread notifications this recipient has. */
    long countByRecipientIdAndReadFalse(UUID recipientId);

    /** Ownership-scoped read so a user can never mutate another recipient's row. */
    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);

    /** Ownership-scoped "all read" marker update — single statement, no row-by-row round-trips. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientId = :recipientId AND n.read = false")
    int markAllRead(@Param("recipientId") UUID recipientId);
}