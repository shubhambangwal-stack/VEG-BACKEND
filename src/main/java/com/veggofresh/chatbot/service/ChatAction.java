package com.veggofresh.chatbot.service;

import java.util.UUID;

/**
 * A resolved customer request.
 *
 * @param intent   what to do
 * @param orderId  order chosen via a button/card ("TRACK:&lt;uuid&gt;"), or null
 * @param orderRef order number typed by the customer (normalised, without "#DM-"), or null
 * @param term     product search text for {@link ChatIntent#PRODUCT}, or null
 */
public record ChatAction(ChatIntent intent, UUID orderId, String orderRef, String term) {

    public static ChatAction of(ChatIntent intent) {
        return new ChatAction(intent, null, null, null);
    }
}
