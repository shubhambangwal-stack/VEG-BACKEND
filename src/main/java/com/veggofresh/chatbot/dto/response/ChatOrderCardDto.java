package com.veggofresh.chatbot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A tappable order card (number, status badge, total, date). Tapping it sends
 * {@code optionId} back, which selects that order for the rest of the chat.
 * {@code status} is the raw order status so the app can colour the badge;
 * {@code statusLabel} is the customer-friendly text to display.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatOrderCardDto {
    private String optionId;
    private UUID orderId;
    private String orderNumber;
    private String status;
    private String statusLabel;
    private BigDecimal total;
    private int itemCount;
    private Instant placedAt;
}
