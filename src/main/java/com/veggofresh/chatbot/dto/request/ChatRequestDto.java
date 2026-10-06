package com.veggofresh.chatbot.dto.request;

import com.veggofresh.chatbot.dto.response.ChatContextDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * One chat turn. Send ONE of:
 * <ul>
 *   <li>nothing / empty body -- start the chat (returns the greeting + latest order)</li>
 *   <li>{@code optionId} -- the customer tapped a button or order card</li>
 *   <li>{@code text} -- the customer typed a message</li>
 * </ul>
 * Always echo back the {@code conversationId} and {@code context} from the previous reply.
 * {@code latitude}/{@code longitude} are optional; when absent the customer's default
 * saved address is used for "deals" and product search.
 */
@Data
public class ChatRequestDto {

    @Size(max = 200, message = "Message is too long (max 200 characters)")
    private String text;

    @Size(max = 80)
    private String optionId;

    @Pattern(regexp = "^[A-Za-z0-9_-]{1,40}$", message = "Invalid conversationId")
    private String conversationId;

    @Valid
    private ChatContextDto context;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;
}
