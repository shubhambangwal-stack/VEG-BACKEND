package com.veggofresh.chatbot.service.impl;

import com.veggofresh.chatbot.dto.request.ChatRatingRequestDto;
import com.veggofresh.chatbot.dto.request.ChatRequestDto;
import com.veggofresh.chatbot.dto.response.ChatContextDto;
import com.veggofresh.chatbot.dto.response.ChatOptionDto;
import com.veggofresh.chatbot.dto.response.ChatOrderCardDto;
import com.veggofresh.chatbot.dto.response.ChatResponseDto;
import com.veggofresh.chatbot.entity.ChatConversationRating;
import com.veggofresh.chatbot.repository.ChatConversationRatingRepository;
import com.veggofresh.chatbot.service.ChatAction;
import com.veggofresh.chatbot.service.ChatIntent;
import com.veggofresh.chatbot.service.ChatOptionFactory;
import com.veggofresh.chatbot.service.ChatRateLimiter;
import com.veggofresh.chatbot.service.ChatbotService;
import com.veggofresh.chatbot.service.IntentMatcher;
import com.veggofresh.chatbot.service.ReplyBuilder;
import com.veggofresh.customer.dto.response.AddressResponseDto;
import com.veggofresh.customer.dto.response.CartResponseDto;
import com.veggofresh.customer.dto.response.CustomerProfileSummaryDto;
import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.customer.service.AddressService;
import com.veggofresh.customer.service.CartService;
import com.veggofresh.customer.service.CustomerProfileService;
import com.veggofresh.customer.service.OrderService;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.ProductDto;
import com.veggofresh.vendor.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates one chat turn: figure out what the customer wants, read the live data
 * through the existing module services, and wrap the answer in a {@link ChatResponseDto}.
 *
 * <p>Deliberately NOT {@code @Transactional}: every downstream service call runs in its
 * own (read-only) transaction, so a "not found" from one of them can never poison a
 * shared transaction. Only {@link #saveRating} opens a transaction.
 *
 * <p>The customer is always the authenticated user passed in by the controller; no id
 * from the request body is ever trusted, and order lookups go through
 * {@code OrderService}, which only returns the caller's own orders.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private static final int RECENT_ORDERS = 5;
    private static final int SUGGESTION_CHIPS = 4;

    private final OrderService orderService;
    private final CartService cartService;
    private final AddressService addressService;
    private final CustomerProfileService customerProfileService;
    private final ProductCatalogService productCatalogService;
    private final ChatConversationRatingRepository ratingRepository;
    private final ChatRateLimiter rateLimiter;
    private final IntentMatcher intentMatcher;
    private final ReplyBuilder replies;
    private final ChatOptionFactory options;

    /** Buttons + context to show after an answer that is not about a specific order. */
    private record FollowUp(List<ChatOptionDto> options, ChatContextDto context) {
    }

    // =====================================================================================
    // Chat turn
    // =====================================================================================

    @Override
    public ChatResponseDto handle(UUID userId, ChatRequestDto request) {
        rateLimiter.check(userId);

        String conversationId = hasText(request.getConversationId())
                ? request.getConversationId()
                : newConversationId();
        ChatAction action = resolveAction(request);

        try {
            return dispatch(userId, conversationId, action, request);
        } catch (BusinessException e) {
            // e.g. ORDER_NOT_FOUND for an id that is not this customer's.
            log.debug("Chatbot lookup failed for user {}: {}", userId, e.getErrorCode());
            FollowUp f = safeFollowUp(userId, request);
            return respond(conversationId, replies.notFound(), f.options(), null, f.context(), false);
        } catch (RuntimeException e) {
            log.error("Chatbot failed for user {}", userId, e);
            FollowUp f = safeFollowUp(userId, request);
            return respond(conversationId, replies.error(), f.options(), null, f.context(), false);
        }
    }

    private ChatAction resolveAction(ChatRequestDto request) {
        if (hasText(request.getOptionId())) {
            return intentMatcher.fromOption(request.getOptionId());
        }
        if (hasText(request.getText())) {
            return intentMatcher.fromText(request.getText());
        }
        return ChatAction.of(ChatIntent.GREETING);
    }

    private ChatResponseDto dispatch(UUID userId, String conv, ChatAction action, ChatRequestDto req) {
        switch (action.intent()) {
            case GREETING:
                return greeting(userId, conv);
            case MENU: {
                FollowUp f = followUp(userId, req);
                return respond(conv, replies.menu(), f.options(), null, f.context(), false);
            }
            case DETAILS:
                return showDetails(userId, conv, req);
            case ORDERS:
                return orderList(userId, conv, req);
            case ORDER_SELECT:
                return selectOrder(userId, conv, action, req);
            case TRACK:
            case PRICE:
            case ITEMS:
            case PARTNER:
            case SHOP:
            case SLOT:
            case INVOICE:
                return orderQuestion(userId, conv, action, req);
            case CANCEL_INFO: {
                FollowUp f = followUp(userId, req);
                return respond(conv, replies.cancelInfo(), f.options(), null, f.context(), false);
            }
            case DEALS:
                return deals(userId, conv, req);
            case CART:
                return cart(userId, conv, req);
            case SEARCH_PROMPT:
                return searchPrompt(userId, conv, req);
            case PRODUCT:
                return product(userId, conv, action.term(), req);
            case END:
                return respond(conv, replies.endMessage(), List.of(), null, endContext(userId, req), true);
            case UNKNOWN:
            default: {
                FollowUp f = followUp(userId, req);
                return respond(conv, replies.unknown(), f.options(), null, f.context(), false);
            }
        }
    }

    // =====================================================================================
    // Handlers
    // =====================================================================================

    private ChatResponseDto greeting(UUID userId, String conv) {
        String firstName = firstName(userId);
        List<OrderResponseDto> latest = recentOrders(userId, 1);
        boolean hasOrders = !latest.isEmpty();

        List<ChatOrderCardDto> cards = hasOrders ? List.of(options.card(latest.get(0))) : List.of();
        return respond(conv, replies.greeting(firstName, hasOrders),
                options.greetingMenu(hasOrders), cards, null, false);
    }

    /** "Yes, show details": the order being discussed, otherwise the most recent one. */
    private ChatResponseDto showDetails(UUID userId, String conv, ChatRequestDto req) {
        Optional<OrderResponseDto> order = contextOrder(userId, req);
        if (order.isEmpty()) {
            List<OrderResponseDto> latest = recentOrders(userId, 1);
            order = latest.isEmpty() ? Optional.empty() : Optional.of(latest.get(0));
        }
        if (order.isEmpty()) {
            return noOrders(conv);
        }
        return orderSelected(conv, order.get());
    }

    private ChatResponseDto orderList(UUID userId, String conv, ChatRequestDto req) {
        List<OrderResponseDto> orders = recentOrders(userId, RECENT_ORDERS);
        if (orders.isEmpty()) {
            return noOrders(conv);
        }
        List<ChatOrderCardDto> cards = orders.stream().map(options::card).collect(Collectors.toList());
        Optional<OrderResponseDto> current = contextOrder(userId, req);
        return respond(conv, replies.orderListIntro(orders.size()),
                List.of(opt("END", "No, that's all")), cards,
                current.map(options::context).orElse(null), false);
    }

    private ChatResponseDto selectOrder(UUID userId, String conv, ChatAction action, ChatRequestDto req) {
        OrderResponseDto order;
        if (action.orderId() != null) {
            order = orderService.getOrderDetails(userId, action.orderId());
        } else {
            order = matchByRef(recentOrders(userId, RECENT_ORDERS), action.orderRef()).orElse(null);
            if (order == null) {
                return respondWithOrderList(userId, conv, replies.orderNotInRecent(RECENT_ORDERS));
            }
        }
        return orderSelected(conv, order);
    }

    private ChatResponseDto orderQuestion(UUID userId, String conv, ChatAction action, ChatRequestDto req) {
        Optional<OrderResponseDto> found = resolveOrder(userId, action, req);
        if (found.isEmpty()) {
            String intro = action.orderRef() != null
                    ? replies.orderNotInRecent(RECENT_ORDERS)
                    : replies.chooseOrder();
            return respondWithOrderList(userId, conv, intro);
        }
        OrderResponseDto o = found.get();
        String reply;
        switch (action.intent()) {
            case TRACK:
                reply = replies.track(o);
                break;
            case PRICE:
                reply = replies.price(o);
                break;
            case ITEMS:
                reply = replies.items(o);
                break;
            case PARTNER:
                reply = replies.partner(o);
                break;
            case SHOP:
                reply = replies.shop(o);
                break;
            case SLOT:
                reply = replies.slot(o);
                break;
            default:
                reply = replies.invoice(o);
                break;
        }
        return respond(conv, reply, options.forOrder(o), null, options.context(o), false);
    }

    private ChatResponseDto deals(UUID userId, String conv, ChatRequestDto req) {
        Optional<double[]> loc = location(userId, req);
        FollowUp f = followUp(userId, req);
        if (loc.isEmpty()) {
            return respond(conv, replies.needAddress(), f.options(), null, f.context(), false);
        }
        List<ProductDto> deals = productCatalogService.getDailyDeals(loc.get()[0], loc.get()[1]);
        return respond(conv, replies.deals(deals), f.options(), null, f.context(), false);
    }

    private ChatResponseDto cart(UUID userId, String conv, ChatRequestDto req) {
        List<CartResponseDto> carts = cartService.getOpenCarts(userId);
        FollowUp f = followUp(userId, req);
        return respond(conv, replies.cart(carts), f.options(), null, f.context(), false);
    }

    private ChatResponseDto searchPrompt(UUID userId, String conv, ChatRequestDto req) {
        // Suggest a few real product names (today's deals) as one-tap chips.
        List<ChatOptionDto> chips = new ArrayList<>();
        try {
            Optional<double[]> loc = location(userId, req);
            if (loc.isPresent()) {
                for (ProductDto p : productCatalogService.getDailyDeals(loc.get()[0], loc.get()[1])) {
                    if (p.getName() != null && !p.getName().isBlank() && chips.size() < SUGGESTION_CHIPS) {
                        chips.add(opt("PRODUCT:" + p.getName(), p.getName()));
                    }
                }
            }
        } catch (RuntimeException e) {
            log.debug("Could not load product suggestions: {}", e.getMessage());
        }
        FollowUp f = followUp(userId, req);
        List<ChatOptionDto> all = new ArrayList<>(chips);
        all.addAll(f.options());
        return respond(conv, replies.searchPrompt(), all, null, f.context(), false);
    }

    private ChatResponseDto product(UUID userId, String conv, String term, ChatRequestDto req) {
        Optional<double[]> loc = location(userId, req);
        FollowUp f = followUp(userId, req);
        if (loc.isEmpty()) {
            return respond(conv, replies.needAddress(), f.options(), null, f.context(), false);
        }
        List<ProductDto> found = productCatalogService
                .searchProducts(term, null, null, null, null, loc.get()[0], loc.get()[1], PageRequest.of(0, 5))
                .getContent();
        return respond(conv, replies.products(term, found), f.options(), null, f.context(), false);
    }

    // =====================================================================================
    // Rating
    // =====================================================================================

    @Override
    @Transactional
    public void saveRating(UUID userId, ChatRatingRequestDto request) {
        boolean skipped = request.isSkipped();
        if (!skipped && request.getRating() == null) {
            throw new BusinessException("RATING_REQUIRED", "Please choose a rating from 1 to 5, or skip.");
        }
        if (ratingRepository.existsByUserIdAndConversationId(userId, request.getConversationId())) {
            throw new BusinessException("ALREADY_RATED", "This conversation has already been rated.",
                    HttpStatus.CONFLICT);
        }

        ChatConversationRating row = new ChatConversationRating();
        row.setUserId(userId);
        row.setConversationId(request.getConversationId());
        row.setSkipped(skipped);
        row.setRatingValue(skipped ? null : request.getRating());
        row.setTags(skipped ? null : joinClean(request.getTags(), "[^A-Za-z0-9 '&.-]", ", ", 300));
        row.setEndReason(request.getEndReason());
        row.setMessageCount(request.getMessageCount());
        row.setOrdersDiscussed(joinClean(request.getOrdersDiscussed(), "[^A-Za-z0-9#-]", ",", 200));

        try {
            ratingRepository.saveAndFlush(row);
        } catch (DataIntegrityViolationException e) {
            // Two taps racing past the exists-check: the unique constraint decides.
            throw new BusinessException("ALREADY_RATED", "This conversation has already been rated.",
                    HttpStatus.CONFLICT);
        }
    }

    // =====================================================================================
    // Data helpers (all reads go through the existing module services)
    // =====================================================================================

    private List<OrderResponseDto> recentOrders(UUID userId, int count) {
        return orderService
                .getOrderHistory(userId, PageRequest.of(0, count, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();
    }

    /** Which order a question is about: tapped id, typed order number, discussed order, or (for tracking) the latest active one. */
    private Optional<OrderResponseDto> resolveOrder(UUID userId, ChatAction action, ChatRequestDto req) {
        if (action.orderId() != null) {
            return Optional.of(orderService.getOrderDetails(userId, action.orderId()));
        }
        if (action.orderRef() != null) {
            return matchByRef(recentOrders(userId, RECENT_ORDERS), action.orderRef());
        }
        Optional<OrderResponseDto> ctx = contextOrder(userId, req);
        if (ctx.isPresent()) {
            return ctx;
        }
        if (action.intent() == ChatIntent.TRACK) {
            return recentOrders(userId, RECENT_ORDERS).stream()
                    .filter(o -> !"DELIVERED".equals(o.getStatus()) && !"CANCELLED".equals(o.getStatus()))
                    .findFirst();
        }
        return Optional.empty();
    }

    /** The order the app says we are talking about, re-read from the database (ownership enforced by OrderService). */
    private Optional<OrderResponseDto> contextOrder(UUID userId, ChatRequestDto req) {
        if (req.getContext() == null || req.getContext().getOrderId() == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(orderService.getOrderDetails(userId, req.getContext().getOrderId()));
        } catch (BusinessException e) {
            return Optional.empty();
        }
    }

    private static Optional<OrderResponseDto> matchByRef(List<OrderResponseDto> orders, String ref) {
        if (ref == null || ref.isBlank()) {
            return Optional.empty();
        }
        String wanted = ref.toUpperCase(Locale.ROOT);
        return orders.stream().filter(o -> {
            String n = o.getOrderNumber() == null ? ""
                    : o.getOrderNumber().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
            if (n.startsWith("DM")) {
                n = n.substring(2);
            }
            return !n.isEmpty() && (n.equals(wanted) || n.endsWith(wanted));
        }).findFirst();
    }

    /** Request coordinates if given, otherwise the customer's default saved address. */
    private Optional<double[]> location(UUID userId, ChatRequestDto req) {
        if (req.getLatitude() != null && req.getLongitude() != null) {
            return Optional.of(new double[]{req.getLatitude(), req.getLongitude()});
        }
        List<AddressResponseDto> addresses = addressService.getAddresses(userId);
        if (addresses == null || addresses.isEmpty()) {
            return Optional.empty();
        }
        AddressResponseDto chosen = addresses.stream()
                .filter(AddressResponseDto::isDefault)
                .findFirst()
                .orElse(addresses.get(0));
        return Optional.of(new double[]{chosen.getLatitude(), chosen.getLongitude()});
    }

    /** Customer's first name for the greeting, or null (we never greet someone by their phone number). */
    private String firstName(UUID userId) {
        try {
            CustomerProfileSummaryDto profile = customerProfileService.getProfileSummary(userId);
            String full = profile == null ? null : profile.getFullName();
            if (full == null || full.isBlank() || full.matches("[+\\d\\s()-]+")) {
                return null;
            }
            return full.trim().split("\\s+")[0];
        } catch (RuntimeException e) {
            return null;
        }
    }

    // =====================================================================================
    // Response helpers
    // =====================================================================================

    private ChatResponseDto orderSelected(String conv, OrderResponseDto o) {
        return respond(conv, replies.selectedOrder(o), options.forOrder(o), null, options.context(o), false);
    }

    private ChatResponseDto noOrders(String conv) {
        return respond(conv, replies.noOrders(), options.greetingMenu(false), null, null, false);
    }

    private ChatResponseDto respondWithOrderList(UUID userId, String conv, String intro) {
        List<OrderResponseDto> orders = recentOrders(userId, RECENT_ORDERS);
        if (orders.isEmpty()) {
            return noOrders(conv);
        }
        List<ChatOrderCardDto> cards = orders.stream().map(options::card).collect(Collectors.toList());
        return respond(conv, intro, List.of(opt("END", "No, that's all")), cards, null, false);
    }

    private FollowUp followUp(UUID userId, ChatRequestDto req) {
        return contextOrder(userId, req)
                .map(o -> new FollowUp(options.forOrder(o), options.context(o)))
                .orElseGet(() -> new FollowUp(options.mainMenu(), null));
    }

    /** followUp that can never throw -- used on the error paths. */
    private FollowUp safeFollowUp(UUID userId, ChatRequestDto req) {
        try {
            return followUp(userId, req);
        } catch (RuntimeException e) {
            return new FollowUp(options.mainMenu(), null);
        }
    }

    private ChatContextDto endContext(UUID userId, ChatRequestDto req) {
        return contextOrder(userId, req).map(options::context).orElse(null);
    }

    private static ChatOptionDto opt(String id, String label) {
        return ChatOptionDto.builder().id(id).label(label).build();
    }

    private static ChatResponseDto respond(String conversationId, String reply, List<ChatOptionDto> opts,
                                           List<ChatOrderCardDto> cards, ChatContextDto context,
                                           boolean ratingPrompt) {
        return ChatResponseDto.builder()
                .conversationId(conversationId)
                .reply(reply)
                .options(opts == null ? List.of() : opts)
                .orders(cards == null ? List.of() : cards)
                .context(context)
                .ratingPrompt(ratingPrompt)
                .build();
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String newConversationId() {
        return "c_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /** Sanitises client-supplied labels before storing them, and caps the total length. */
    private static String joinClean(List<String> values, String stripRegex, String delimiter, int maxLen) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        String joined = values.stream()
                .filter(v -> v != null)
                .map(v -> v.replaceAll(stripRegex, "").trim())
                .filter(v -> !v.isEmpty())
                .collect(Collectors.joining(delimiter));
        if (joined.isEmpty()) {
            return null;
        }
        return joined.length() > maxLen ? joined.substring(0, maxLen) : joined;
    }
}
