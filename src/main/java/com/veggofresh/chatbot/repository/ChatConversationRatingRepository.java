package com.veggofresh.chatbot.repository;

import com.veggofresh.chatbot.entity.ChatConversationRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChatConversationRatingRepository extends JpaRepository<ChatConversationRating, UUID> {

    boolean existsByUserIdAndConversationId(UUID userId, String conversationId);
}
