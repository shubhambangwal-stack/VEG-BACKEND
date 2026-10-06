package com.veggofresh.chatbot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * "Which order are we talking about" -- returned with every reply so the app can
 * show a "Talking about #DM-..." bar, and echoed back by the app on the next
 * request so typed follow-ups ("what's in it?") know which order is meant.
 *
 * <p>Only {@code orderId} is ever read from a request. The other fields are
 * display-only and are always rebuilt from the database by the server.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatContextDto {
    private UUID orderId;
    private String orderNumber;
    private String statusLabel;
}
