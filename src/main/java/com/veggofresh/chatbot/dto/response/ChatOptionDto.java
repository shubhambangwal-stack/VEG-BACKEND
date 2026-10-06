package com.veggofresh.chatbot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One tappable button. The app shows {@code label} and sends {@code id} back as {@code optionId}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatOptionDto {
    private String id;
    private String label;
}
