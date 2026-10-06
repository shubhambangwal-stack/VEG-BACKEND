package com.veggofresh.chatbot.service;

import com.veggofresh.chatbot.dto.request.ChatRatingRequestDto;
import com.veggofresh.chatbot.dto.request.ChatRequestDto;
import com.veggofresh.chatbot.dto.response.ChatResponseDto;

import java.util.UUID;

/**
 * Read-only customer support chatbot. Answers come live from the order, cart,
 * address and catalog services; nothing is stored except the final rating.
 */
public interface ChatbotService {

    /** Handles one chat turn (start, tapped option, or typed text) for the given customer. */
    ChatResponseDto handle(UUID userId, ChatRequestDto request);

    /** Records the customer's rating (or skip) of a finished conversation. */
    void saveRating(UUID userId, ChatRatingRequestDto request);
}
