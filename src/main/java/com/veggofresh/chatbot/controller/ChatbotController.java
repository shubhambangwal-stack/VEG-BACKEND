package com.veggofresh.chatbot.controller;

import com.veggofresh.chatbot.dto.request.ChatRatingRequestDto;
import com.veggofresh.chatbot.dto.request.ChatRequestDto;
import com.veggofresh.chatbot.dto.response.ChatResponseDto;
import com.veggofresh.chatbot.service.ChatbotService;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer support chatbot (read-only).
 *
 * <ul>
 *   <li>{@code POST /api/customer/chat} -- one chat turn. An empty body starts the chat.</li>
 *   <li>{@code POST /api/customer/chat/rating} -- rate (or skip) a finished conversation.</li>
 * </ul>
 *
 * The customer is always taken from the security context, never from the request body.
 */
@RestController
@RequestMapping("/api/customer/chat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponseDto>> chat(
            @Valid @RequestBody(required = false) ChatRequestDto request) {
        ChatRequestDto turn = request != null ? request : new ChatRequestDto();
        ChatResponseDto response = chatbotService.handle(SecurityUtils.getCurrentUserId(), turn);
        return ResponseEntity.ok(ApiResponse.success(response, "OK"));
    }

    @PostMapping("/rating")
    public ResponseEntity<ApiResponse<Void>> rate(@Valid @RequestBody ChatRatingRequestDto request) {
        chatbotService.saveRating(SecurityUtils.getCurrentUserId(), request);
        ApiResponse<Void> body = ApiResponse.success("Thanks for your feedback!");
        return ResponseEntity.ok(body);
    }
}
