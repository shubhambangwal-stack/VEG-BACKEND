package com.veggofresh.chatbot.service;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a tapped option id or a typed English message into a {@link ChatAction}.
 *
 * <p>Plain keyword/regex matching -- no AI model, no external service. Tapped
 * options ("TRACK:&lt;uuid&gt;") are exact; typed text is best-effort and falls back
 * to a product search (for short product-like phrases) or the menu.
 */
@Component
public class IntentMatcher {

    private static final int MAX_TEXT = 200;
    private static final int MAX_TERM = 40;

    private static final Pattern END = Pattern.compile(
            "\\b(thanks?|thank you|thx|bye|goodbye|that'?s all|that is all|thats all|no more|nothing else|no thanks|all good|i'?m done|im done)\\b|^done$");
    /** Words that mean the customer is asking something real, so "thanks, where is my order" is not an ending. */
    private static final Pattern STRONG_INTENT = Pattern.compile(
            "\\b(where|track|tracking|status|paid|pay|total|price|prices|items?|invoice|bill|cancel|refund|rider|partner|shop|slot|address|deals?|offers?|cart|orders)\\b");
    private static final Pattern GREETING_PREFIX = Pattern.compile(
            "^(hi+|hello+|hey+|hiya|namaste|good (morning|afternoon|evening))\\b[\\s,!.:-]*");
    private static final Pattern GREETING_FILLER = Pattern.compile(
            "\\b(there|bot|team|support|friend|sir|madam|assistant|how are you|how r u)\\b");
    private static final Pattern MENU = Pattern.compile(
            "(menu|help|options|main menu|start|what can you do.*)");
    private static final Pattern DETAILS = Pattern.compile(
            "^(yes|yeah|yep|yup|sure|ok|okay)\\b[\\s,.!]*(please|pls|go ahead|continue|show details?|show more|more details?|details?)?[\\s,.!]*$"
                    + "|\\b(show|see|view|give)( me)?( the)?( more)? details?\\b|\\bmore details?\\b");
    private static final Pattern SEARCH = Pattern.compile(
            "\\b(search|find|look for)( for)?( a| an)? products?\\b");
    private static final Pattern CART = Pattern.compile("\\b(cart|basket)\\b");
    private static final Pattern DEALS = Pattern.compile("\\b(deals?|offers?|discounts?|sale)\\b");
    private static final Pattern ORDERISH = Pattern.compile(
            "\\b(order|orders|bill|invoice|paid|pay|payment|total|track|status|rider|partner|agent|shop|store|slot|address|items?|delivery|delivered|cancel|refund)\\b");
    private static final Pattern ORDERS = Pattern.compile(
            "\\b(my orders|orders|order history|history|recent orders?|past orders?|previous orders?|another order|other orders?|change order|all orders)\\b");
    private static final Pattern ORDER_REF = Pattern.compile(
            "(?<![a-z0-9])(?:#\\s*)?dm\\s*-?\\s*([a-z0-9]{4,20})");
    private static final Pattern INVOICE = Pattern.compile("\\b(invoice|receipt|bill)\\b");
    private static final Pattern SLOT = Pattern.compile(
            "\\b(address|slot|scheduled|delivering to|deliver(ed)? to)\\b");
    private static final Pattern CANCEL = Pattern.compile("\\b(cancel|cancellation|refund)\\b");
    private static final Pattern PARTNER = Pattern.compile(
            "\\b(rider|partner|agent|driver|delivery (boy|person|guy|man|executive)|who('s| is)? (delivering|bringing)|who delivers?)\\b");
    private static final Pattern SHOP = Pattern.compile("\\b(shop|store|vendor|seller)\\b");
    private static final Pattern PRICE_ORDER = Pattern.compile(
            "\\b(paid|pay|payment|total|amount|charged|breakdown|fees?|price details|how much did)\\b");
    private static final Pattern ITEMS = Pattern.compile(
            "\\bitems?\\b|\\bwhat('s| is| was)? (in|inside)\\b|\\bwhat (did )?i (order|ordered|buy|bought)\\b|\\bcontents?\\b");
    private static final Pattern TRACK = Pattern.compile(
            "\\b(where|track|tracking|status|arrive|arriving|arrival|eta|late|delayed|how long|reach)\\b|\\bwhen\\b.*\\b(order|it|come|arrive|deliver|delivered|reach)\\b");

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "price", "prices", "of", "the", "a", "an", "is", "are", "for", "do", "you", "have", "got", "any",
            "available", "availability", "stock", "in", "near", "me", "my", "how", "much", "cost", "costs",
            "what", "whats", "show", "find", "looking", "i", "am", "want", "to", "buy", "get", "can", "please",
            "pls", "tell", "about", "today", "now", "there", "fresh", "rate", "rates", "per", "kg", "and"));

    /** Resolves a tapped option id such as {@code TRACK:<order uuid>} or {@code ORDERS}. */
    public ChatAction fromOption(String optionId) {
        String id = optionId == null ? "" : optionId.trim();
        int colon = id.indexOf(':');
        String type = (colon < 0 ? id : id.substring(0, colon)).toUpperCase(Locale.ROOT);
        String arg = colon < 0 ? null : id.substring(colon + 1).trim();
        UUID uuid = parseUuid(arg);

        switch (type) {
            case "MENU":
                return ChatAction.of(ChatIntent.MENU);
            case "DETAILS":
                return ChatAction.of(ChatIntent.DETAILS);
            case "ORDERS":
                return ChatAction.of(ChatIntent.ORDERS);
            case "DEALS":
                return ChatAction.of(ChatIntent.DEALS);
            case "CART":
                return ChatAction.of(ChatIntent.CART);
            case "SEARCH":
                return ChatAction.of(ChatIntent.SEARCH_PROMPT);
            case "END":
                return ChatAction.of(ChatIntent.END);
            case "ORDER":
                return uuid == null ? ChatAction.of(ChatIntent.ORDERS)
                        : new ChatAction(ChatIntent.ORDER_SELECT, uuid, null, null);
            case "TRACK":
                return new ChatAction(ChatIntent.TRACK, uuid, null, null);
            case "PRICE":
                return new ChatAction(ChatIntent.PRICE, uuid, null, null);
            case "ITEMS":
                return new ChatAction(ChatIntent.ITEMS, uuid, null, null);
            case "PARTNER":
                return new ChatAction(ChatIntent.PARTNER, uuid, null, null);
            case "SHOP":
                return new ChatAction(ChatIntent.SHOP, uuid, null, null);
            case "SLOT":
                return new ChatAction(ChatIntent.SLOT, uuid, null, null);
            case "INVOICE":
                return new ChatAction(ChatIntent.INVOICE, uuid, null, null);
            case "PRODUCT": {
                String term = arg == null ? "" : cleanTerm(arg);
                return term.isEmpty() ? ChatAction.of(ChatIntent.SEARCH_PROMPT)
                        : new ChatAction(ChatIntent.PRODUCT, null, null, term);
            }
            default:
                return ChatAction.of(ChatIntent.UNKNOWN);
        }
    }

    /** Resolves a typed message. */
    public ChatAction fromText(String raw) {
        String q = normalize(raw);
        if (q.isEmpty()) {
            return ChatAction.of(ChatIntent.GREETING);
        }
        if (q.length() <= 60 && END.matcher(q).find() && !STRONG_INTENT.matcher(q).find()) {
            return ChatAction.of(ChatIntent.END);
        }
        // "hello" alone is a greeting; "hello, where is my order" is a question that starts politely.
        Matcher greet = GREETING_PREFIX.matcher(q);
        if (greet.find()) {
            String rest = q.substring(greet.end()).trim();
            String meaningful = GREETING_FILLER.matcher(rest).replaceAll(" ").replaceAll("[^a-z0-9]", "");
            if (meaningful.isEmpty()) {
                return ChatAction.of(ChatIntent.GREETING);
            }
            q = rest;
        }
        if (MENU.matcher(q).matches()) {
            return ChatAction.of(ChatIntent.MENU);
        }
        if (DETAILS.matcher(q).find()) {
            return ChatAction.of(ChatIntent.DETAILS);
        }
        if (SEARCH.matcher(q).find()) {
            return ChatAction.of(ChatIntent.SEARCH_PROMPT);
        }
        if (CART.matcher(q).find()) {
            return ChatAction.of(ChatIntent.CART);
        }

        String ref = extractOrderRef(q);
        boolean orderish = ORDERISH.matcher(q).find();
        if (!orderish && DEALS.matcher(q).find()) {
            return ChatAction.of(ChatIntent.DEALS);
        }
        if (ref != null && onlyOrderRef(q)) {
            return new ChatAction(ChatIntent.ORDER_SELECT, null, ref, null);
        }
        if (ORDERS.matcher(q).find()) {
            return ChatAction.of(ChatIntent.ORDERS);
        }

        ChatIntent intent = orderIntent(q);
        if (intent != null) {
            return new ChatAction(intent, null, ref, null);
        }

        String term = productTerm(q);
        if (!term.isEmpty()) {
            return new ChatAction(ChatIntent.PRODUCT, null, null, term);
        }
        return ChatAction.of(ChatIntent.UNKNOWN);
    }

    private static ChatIntent orderIntent(String q) {
        if (INVOICE.matcher(q).find()) return ChatIntent.INVOICE;
        if (SLOT.matcher(q).find()) return ChatIntent.SLOT;
        if (CANCEL.matcher(q).find()) return ChatIntent.CANCEL_INFO;
        if (PARTNER.matcher(q).find()) return ChatIntent.PARTNER;
        if (SHOP.matcher(q).find()) return ChatIntent.SHOP;
        if (PRICE_ORDER.matcher(q).find()) return ChatIntent.PRICE;
        if (ITEMS.matcher(q).find()) return ChatIntent.ITEMS;
        if (TRACK.matcher(q).find()) return ChatIntent.TRACK;
        // "my order" with nothing else -- the customer most likely wants its status.
        if (q.contains("order")) return ChatIntent.TRACK;
        return null;
    }

    private static String extractOrderRef(String q) {
        Matcher m = ORDER_REF.matcher(q);
        return m.find() ? m.group(1).toUpperCase(Locale.ROOT) : null;
    }

    private static boolean onlyOrderRef(String q) {
        String rest = ORDER_REF.matcher(q).replaceAll(" ")
                .replaceAll("\\b(order|my|the|show|about|details?|for|of|me|number|no)\\b", " ")
                .replaceAll("[^a-z0-9]", "");
        return rest.isEmpty();
    }

    /** Strips filler words ("price of", "do you have") and returns what is left as a product name. */
    private static String productTerm(String q) {
        String[] tokens = q.replaceAll("[^a-z0-9 ]", " ").trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        int kept = 0;
        for (String t : tokens) {
            if (t.isEmpty() || STOP_WORDS.contains(t)) {
                continue;
            }
            kept++;
            if (sb.length() > 0) sb.append(' ');
            sb.append(t);
        }
        // A long leftover is a sentence we did not understand, not a product name.
        if (kept == 0 || kept > 4) {
            return "";
        }
        return cleanTerm(sb.toString());
    }

    private static String cleanTerm(String term) {
        String t = term.replaceAll("[^A-Za-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        return t.length() > MAX_TERM ? t.substring(0, MAX_TERM).trim() : t;
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'')
                .replaceAll("\\s+", " ")
                .trim();
        return s.length() > MAX_TEXT ? s.substring(0, MAX_TEXT) : s;
    }

    private static UUID parseUuid(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
