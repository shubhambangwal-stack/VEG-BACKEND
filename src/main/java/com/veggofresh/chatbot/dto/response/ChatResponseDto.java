package com.veggofresh.chatbot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Every chat reply has this one shape.
 *
 * <ul>
 *   <li>{@code reply} -- the bot's message text (plain text, line breaks are \n).</li>
 *   <li>{@code orders} -- order cards to render under the message (may be empty).</li>
 *   <li>{@code options} -- tappable buttons to render under the message (may be empty).</li>
 *   <li>{@code context} -- the order being discussed, or null. Echo it back on the next request.</li>
 *   <li>{@code conversationId} -- echo it back on every request and on the rating call.</li>
 *   <li>{@code ratingPrompt} -- true when the chat has ended: show the star rating UI.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDto {
    private String conversationId;
    private String reply;
    private List<ChatOptionDto> options;
    private List<ChatOrderCardDto> orders;
    private ChatContextDto context;
    private boolean ratingPrompt;
}
