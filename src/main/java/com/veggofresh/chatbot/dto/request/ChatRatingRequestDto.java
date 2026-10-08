package com.veggofresh.chatbot.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Rating of a finished conversation. Either {@code rating} (1-5) or {@code skipped = true}.
 * {@code tags}, {@code messageCount} and {@code ordersDiscussed} are client-reported analytics.
 */
@Data
public class ChatRatingRequestDto {

    @NotBlank(message = "conversationId is required")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,40}$", message = "Invalid conversationId")
    private String conversationId;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    private boolean skipped;

    @Size(max = 5, message = "At most 5 tags")
    private List<@Size(max = 40) String> tags;

    @Pattern(regexp = "^[a-z_]{1,30}$", message = "Invalid endReason")
    private String endReason;

    @Min(0)
    @Max(10000)
    private Integer messageCount;

    @Size(max = 5)
    private List<@Size(max = 30) String> ordersDiscussed;
}
