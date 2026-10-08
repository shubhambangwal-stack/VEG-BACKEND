package com.veggofresh.chatbot.service;

/** What the customer wants. Produced by {@link IntentMatcher}, consumed by ChatbotServiceImpl. */
public enum ChatIntent {
    GREETING,
    MENU,
    /** "Yes, show details" -- open the latest (or currently discussed) order. */
    DETAILS,
    /** Show the last 5 orders as tappable cards. */
    ORDERS,
    /** A specific order was picked (card tap or typed order number). */
    ORDER_SELECT,
    TRACK,
    PRICE,
    ITEMS,
    PARTNER,
    SHOP,
    SLOT,
    INVOICE,
    /** Cancel / refund questions -- the bot only explains where to do it. */
    CANCEL_INFO,
    DEALS,
    CART,
    /** "Search a product" button -- asks which product. */
    SEARCH_PROMPT,
    PRODUCT,
    END,
    UNKNOWN
}
