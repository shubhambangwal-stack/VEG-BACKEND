package com.veggofresh.chatbot.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.util.UUID;

/** A customer's rating (or skip) of one chat conversation. See migration V162. */
@Entity
@Table(name = "chat_conversation_ratings")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class ChatConversationRating extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "conversation_id", nullable = false, length = 40)
    private String conversationId;

    /** 1-5, or null when the customer skipped. */
    @Column(name = "rating_value")
    private Integer ratingValue;

    @Column(nullable = false)
    private boolean skipped;

    /** Comma-separated reason tags, e.g. "Quick answers, Helpful". */
    @Column(length = 300)
    private String tags;

    @Column(name = "end_reason", length = 30)
    private String endReason;

    @Column(name = "message_count")
    private Integer messageCount;

    /** Comma-separated order numbers the customer asked about (client-reported). */
    @Column(name = "orders_discussed", length = 200)
    private String ordersDiscussed;
}
