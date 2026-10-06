package com.veggofresh.chatbot.service;

import com.veggofresh.customer.dto.response.CartItemResponseDto;
import com.veggofresh.customer.dto.response.CartResponseDto;
import com.veggofresh.customer.dto.response.OrderItemResponseDto;
import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.vendor.dto.ProductDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * All customer-facing wording lives here. Every method is a pure function of the
 * data it is given: it never calculates money (it prints the amounts stored on the
 * order) and never invents data -- a missing value is reported as "not yet", not guessed.
 */
@Component
public class ReplyBuilder {

    private static final String RUPEE = "\u20B9";
    private static final String MINUS = "\u2212";
    private static final String DONE = "\u2713 ";
    private static final String NOW = "\u2192 ";
    private static final String LATER = "\u25CB ";
    private static final int MAX_LINES = 12;

    private static final String[] STEPS = {
            "Order placed", "Shop accepted", "Being prepared",
            "Delivery partner accepted", "Out for delivery", "Delivered"
    };

    private final DateTimeFormatter dateFormat;
    private final String supportContact;

    public ReplyBuilder(@Value("${veggofresh.chatbot.timezone:Asia/Kolkata}") String timezone,
                        @Value("${veggofresh.chatbot.support-contact:}") String supportContact) {
        this.dateFormat = DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH)
                .withZone(ZoneId.of(timezone));
        this.supportContact = supportContact == null ? "" : supportContact.trim();
    }

    // ------------------------------------------------------------------ small helpers

    public static boolean hasAgent(OrderResponseDto o) {
        return o.getDeliveryAgentName() != null && !o.getDeliveryAgentName().isBlank();
    }

    private static String st(OrderResponseDto o) {
        return o.getStatus() == null ? "" : o.getStatus().toUpperCase(Locale.ROOT);
    }

    /** "Order #DM-..." for the start of a sentence. */
    private static String ref(OrderResponseDto o) {
        return o.getOrderNumber() != null ? "Order " + o.getOrderNumber() : "This order";
    }

    /** "order #DM-..." for the middle of a sentence. */
    private static String refLower(OrderResponseDto o) {
        return o.getOrderNumber() != null ? "order " + o.getOrderNumber() : "this order";
    }

    private static boolean positive(BigDecimal v) {
        return v != null && v.signum() > 0;
    }

    public String money(BigDecimal v) {
        if (v == null) {
            return RUPEE + "0";
        }
        BigDecimal r = v.setScale(2, RoundingMode.HALF_UP);
        return r.stripTrailingZeros().scale() <= 0
                ? RUPEE + r.toBigInteger()
                : RUPEE + r.toPlainString();
    }

    private String when(java.time.Instant t) {
        return t == null ? "" : dateFormat.format(t);
    }

    private String support() {
        return supportContact.isEmpty() ? "" : " (" + supportContact + ")";
    }

    private static String paymentLabel(String method) {
        if (method == null || method.isBlank()) {
            return null;
        }
        switch (method.trim().toUpperCase(Locale.ROOT)) {
            case "COD":
                return "Cash on delivery";
            case "ONLINE":
                return "Online payment";
            case "UPI":
                return "UPI";
            case "WALLET":
                return "Wallet";
            default:
                return method.trim();
        }
    }

    private BigDecimal subtotal(OrderResponseDto o) {
        BigDecimal s = BigDecimal.ZERO;
        if (o.getItems() != null) {
            for (OrderItemResponseDto i : o.getItems()) {
                if (i.getSubTotal() != null) {
                    s = s.add(i.getSubTotal());
                }
            }
        }
        return s;
    }

    private String itemLine(OrderItemResponseDto i) {
        String unit = i.getUnit() == null || i.getUnit().isBlank() ? "" : " (" + i.getUnit() + ")";
        return "- " + i.getProductName() + unit + " x " + i.getQuantity() + " = " + money(i.getSubTotal());
    }

    // ------------------------------------------------------------------ status wording

    /** Short customer-friendly status, used on order cards and in the "talking about" bar. */
    public String statusLabel(OrderResponseDto o) {
        switch (st(o)) {
            case "PLACED":
                return "Waiting for a shop";
            case "CONFIRMED":
                return "Shop accepted";
            case "PREPARING":
                return "Being prepared";
            case "READY_FOR_PICKUP":
                return hasAgent(o) ? "Partner heading to shop" : "Finding delivery partner";
            case "OUT_FOR_DELIVERY":
                return "Out for delivery";
            case "DELIVERED":
                return "Delivered";
            case "CANCELLED":
                return "Cancelled";
            default:
                return "Processing";
        }
    }

    // ------------------------------------------------------------------ greetings / generic

    public String greeting(String firstName, boolean hasOrders) {
        String hi = firstName == null ? "Hi there!" : "Hi " + firstName + "!";
        return hasOrders
                ? hi + " Here's your most recent order. Want to see more about it?"
                : hi + " I can help with your orders, cart and products. What would you like to know?";
    }

    public String menu() {
        return "What would you like to know?";
    }

    public String noOrders() {
        return "You haven't placed any orders yet. I can still help with today's deals and product prices.";
    }

    public String orderListIntro(int count) {
        return count == 1
                ? "Here is your order. Tap it to talk about it:"
                : "Here are your last " + count + " orders. Tap one to talk about it:";
    }

    public String orderNotInRecent(int n) {
        return "I couldn't find that order in your last " + n + ". Pick one from your list:";
    }

    public String chooseOrder() {
        return "Which order do you mean? Tap one:";
    }

    public String selectedOrder(OrderResponseDto o) {
        return "Got it, " + refLower(o) + ": " + statusLabel(o).toLowerCase(Locale.ROOT)
                + ", total " + money(o.getTotalAmount()) + ".\nWhat would you like to know about it?";
    }

    public String notFound() {
        return "Sorry, I couldn't find that. Here's what I can help with:";
    }

    public String error() {
        return "Sorry, something went wrong while fetching that. Please try again in a moment.";
    }

    public String unknown() {
        return "I'm not sure about that one. Here's what I can help with:";
    }

    public String endMessage() {
        return "Glad I could help! How would you rate this conversation?";
    }

    public String cancelInfo() {
        return "I can't cancel orders or handle refunds in this chat. While an order is waiting for a shop "
                + "or has just been accepted, you can cancel it from the order screen in the app. "
                + "For refunds or any payment question, please contact support" + support() + ".";
    }

    public String searchPrompt() {
        return "Which product are you looking for? Type a name, for example tomato.";
    }

    public String needAddress() {
        return "I need a delivery address to check what's available near you. "
                + "Please add or select an address in the app and try again.";
    }

    // ------------------------------------------------------------------ order answers

    public String track(OrderResponseDto o) {
        String status = st(o);
        if ("CANCELLED".equals(status)) {
            return ref(o) + " was cancelled. For payment questions, please contact support" + support() + ".";
        }

        boolean agent = hasAgent(o);
        int cur;
        String note;
        switch (status) {
            case "PLACED":
                cur = 1;
                note = "waiting for a nearby shop";
                break;
            case "CONFIRMED":
                cur = 2;
                note = "the shop will start preparing soon";
                break;
            case "PREPARING":
                cur = 2;
                note = "the shop is preparing your items";
                break;
            case "READY_FOR_PICKUP":
                cur = agent ? 4 : 3;
                note = agent ? o.getDeliveryAgentName() + " is heading to the shop to pick it up"
                        : "finding a delivery partner";
                break;
            case "OUT_FOR_DELIVERY":
                cur = 4;
                note = o.getEstimatedDeliveryWindow() != null && !o.getEstimatedDeliveryWindow().isBlank()
                        ? "on the way, expected " + o.getEstimatedDeliveryWindow()
                        : "on the way";
                break;
            case "DELIVERED":
                cur = STEPS.length;
                note = null;
                break;
            default:
                cur = 1;
                note = "being processed";
                break;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(ref(o)).append(": ").append(statusLabel(o)).append("\n");
        for (int i = 0; i < STEPS.length; i++) {
            String prefix = i < cur ? DONE : (i == cur ? NOW : LATER);
            sb.append(prefix).append(STEPS[i]);
            if (i == 1 && i < cur && o.getShopName() != null) {
                sb.append(" (").append(o.getShopName()).append(")");
            }
            if (i == 3 && i < cur && agent) {
                sb.append(" (").append(o.getDeliveryAgentName()).append(")");
            }
            if (i == cur && note != null) {
                sb.append(" (").append(note).append(")");
            }
            if (i < STEPS.length - 1) {
                sb.append("\n");
            }
        }

        if ("PLACED".equals(status)) {
            sb.append("\nYou'll get a notification as soon as a shop accepts.");
        } else if ("READY_FOR_PICKUP".equals(status) && !agent) {
            sb.append("\nNearby delivery partners have been notified. The first one to accept will pick it up.");
        } else if ("DELIVERED".equals(status) && o.getCreatedAt() != null) {
            sb.append("\nPlaced on ").append(when(o.getCreatedAt())).append(".");
        }
        return sb.toString();
    }

    public String price(OrderResponseDto o) {
        StringBuilder sb = new StringBuilder();
        sb.append("For ").append(refLower(o)).append(" the total is ")
                .append(money(o.getTotalAmount())).append(".\n");

        List<String> parts = new ArrayList<>();
        parts.add("Items " + money(subtotal(o)));
        if (o.getDeliveryFee() != null) {
            parts.add("Delivery " + money(o.getDeliveryFee()));
        }
        if (o.getEstimatedTax() != null) {
            // The order stores the flat platform fee in the "estimatedTax" column.
            parts.add("Platform fee " + money(o.getEstimatedTax()));
        }
        String line = String.join(" + ", parts);
        if (positive(o.getPromoDiscount())) {
            String code = o.getPromoCode() == null || o.getPromoCode().isBlank() ? "" : " (" + o.getPromoCode() + ")";
            line += " " + MINUS + " Promo" + code + " " + money(o.getPromoDiscount());
        }
        sb.append(line).append(".");

        String pay = paymentLabel(o.getPaymentMethod());
        if (pay != null) {
            sb.append("\nPayment method: ").append(pay).append(".");
        }
        return sb.toString();
    }

    public String items(OrderResponseDto o) {
        List<OrderItemResponseDto> items = o.getItems();
        if (items == null || items.isEmpty()) {
            return ref(o) + " has no items on record.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(ref(o)).append(" has ").append(items.size())
                .append(items.size() == 1 ? " item:" : " items:");
        int shown = 0;
        for (OrderItemResponseDto i : items) {
            if (shown++ >= MAX_LINES) {
                sb.append("\n...and ").append(items.size() - MAX_LINES).append(" more");
                break;
            }
            sb.append("\n").append(itemLine(i));
        }
        return sb.toString();
    }

    public String partner(OrderResponseDto o) {
        String status = st(o);
        if ("CANCELLED".equals(status)) {
            return "No delivery was arranged because this order was cancelled.";
        }
        if (hasAgent(o)) {
            String agent = o.getDeliveryAgentName();
            String first;
            switch (status) {
                case "DELIVERED":
                    first = agent + " delivered " + refLower(o) + ".";
                    break;
                case "OUT_FOR_DELIVERY":
                    first = agent + " is delivering " + refLower(o) + ".";
                    break;
                case "READY_FOR_PICKUP":
                    first = agent + " accepted " + refLower(o) + " and is heading to the shop to pick it up.";
                    break;
                default:
                    first = agent + " is your delivery partner for " + refLower(o) + ".";
                    break;
            }
            StringBuilder sb = new StringBuilder(first);
            if (o.getDeliveryAgentPhone() != null && !o.getDeliveryAgentPhone().isBlank()) {
                sb.append(" You can call ").append(agent).append(" on ").append(o.getDeliveryAgentPhone()).append(".");
            }
            if (o.getShopName() != null) {
                sb.append("\nThe shop is ").append(o.getShopName());
                if (o.getShopBusinessPhone() != null && !o.getShopBusinessPhone().isBlank()) {
                    sb.append(" (").append(o.getShopBusinessPhone()).append(")");
                }
                sb.append(".");
            }
            return sb.toString();
        }
        switch (status) {
            case "PLACED":
                return "No delivery partner yet. One is assigned after a shop accepts and prepares your order.";
            case "READY_FOR_PICKUP":
                return "No delivery partner yet. Nearby partners have been notified, "
                        + "and the first one to accept will pick up your order.";
            case "DELIVERED":
                return "I don't have the delivery partner's details for this order.";
            default:
                return "No delivery partner yet. One is assigned once the shop has your order ready.";
        }
    }

    public String shop(OrderResponseDto o) {
        if (o.getShopName() != null) {
            StringBuilder sb = new StringBuilder(ref(o)).append(" is with ").append(o.getShopName());
            if (o.getShopBusinessPhone() != null && !o.getShopBusinessPhone().isBlank()) {
                sb.append(". You can call them on ").append(o.getShopBusinessPhone());
            }
            return sb.append(".").toString();
        }
        if ("CANCELLED".equals(st(o))) {
            return "No shop accepted this order.";
        }
        return "No shop has accepted " + refLower(o) + " yet.";
    }

    public String slot(OrderResponseDto o) {
        StringBuilder sb = new StringBuilder();
        sb.append(ref(o)).append(" goes to: ").append(o.getDeliveryAddress()).append(".\n");

        List<String> parts = new ArrayList<>();
        if (o.getScheduledDate() != null && !o.getScheduledDate().isBlank()) {
            parts.add(o.getScheduledDate());
        }
        if (o.getDeliveryTimeSlot() != null && !o.getDeliveryTimeSlot().isBlank()) {
            parts.add(o.getDeliveryTimeSlot());
        }
        if (parts.isEmpty()) {
            sb.append("No specific delivery slot was chosen.");
        } else {
            sb.append("Delivery slot: ").append(String.join(", ", parts)).append(".");
        }
        if (o.getEstimatedDeliveryWindow() != null && !o.getEstimatedDeliveryWindow().isBlank()) {
            sb.append("\nEstimated arrival: ").append(o.getEstimatedDeliveryWindow()).append(".");
        }
        return sb.toString();
    }

    public String invoice(OrderResponseDto o) {
        StringBuilder sb = new StringBuilder("Bill summary for ").append(refLower(o)).append(":");
        if (o.getItems() != null) {
            int shown = 0;
            for (OrderItemResponseDto i : o.getItems()) {
                if (shown++ >= MAX_LINES) {
                    sb.append("\n...and ").append(o.getItems().size() - MAX_LINES).append(" more");
                    break;
                }
                sb.append("\n").append(itemLine(i));
            }
        }
        if (o.getDeliveryFee() != null) {
            sb.append("\nDelivery ").append(money(o.getDeliveryFee()));
        }
        if (o.getEstimatedTax() != null) {
            sb.append("\nPlatform fee ").append(money(o.getEstimatedTax()));
        }
        if (positive(o.getPromoDiscount())) {
            String code = o.getPromoCode() == null || o.getPromoCode().isBlank() ? "" : " (" + o.getPromoCode() + ")";
            sb.append("\nPromo").append(code).append(" ").append(MINUS).append(money(o.getPromoDiscount()));
        }
        sb.append("\nTotal ").append(money(o.getTotalAmount()));
        String pay = paymentLabel(o.getPaymentMethod());
        if (pay != null) {
            sb.append("\nPayment method: ").append(pay);
        }
        sb.append("\nYou can also download the PDF invoice from the order screen in the app.");
        return sb.toString();
    }

    // ------------------------------------------------------------------ cart / catalog answers

    public String cart(List<CartResponseDto> carts) {
        int totalItems = 0;
        if (carts != null) {
            for (CartResponseDto c : carts) {
                if (c.getItems() != null) {
                    totalItems += c.getItems().size();
                }
            }
        }
        if (carts == null || totalItems == 0) {
            return "Your cart is empty. Ask me about today's deals or search for a product.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Your cart has ").append(totalItems).append(totalItems == 1 ? " item:" : " items:");
        int cartNo = 0;
        for (CartResponseDto c : carts) {
            if (c.getItems() == null || c.getItems().isEmpty()) {
                continue;
            }
            if (++cartNo > 3) {
                sb.append("\n...and more carts in the app");
                break;
            }
            if (carts.size() > 1 && c.getCartLabel() != null && !c.getCartLabel().isBlank()) {
                sb.append("\n").append(c.getCartLabel()).append(":");
            }
            int shown = 0;
            for (CartItemResponseDto i : c.getItems()) {
                if (shown++ >= 8) {
                    sb.append("\n...and ").append(c.getItems().size() - 8).append(" more");
                    break;
                }
                String unit = i.getUnit() == null || i.getUnit().isBlank() ? "" : " (" + i.getUnit() + ")";
                sb.append("\n- ").append(i.getProductName()).append(unit)
                        .append(" x ").append(i.getQuantity()).append(" = ").append(money(i.getSubTotal()));
            }
            BigDecimal payable = c.getPayableAmount() != null ? c.getPayableAmount() : c.getTotalAmount();
            sb.append("\nTotal to pay ").append(money(payable));
            if (c.getUnavailableItemCount() > 0) {
                sb.append(" (").append(c.getUnavailableItemCount())
                        .append(c.getUnavailableItemCount() == 1 ? " item is" : " items are")
                        .append(" currently unavailable)");
            }
        }
        sb.append("\nOpen the cart in the app to check out.");
        return sb.toString();
    }

    public String deals(List<ProductDto> deals) {
        if (deals == null || deals.isEmpty()) {
            return "There are no special deals near you right now. Check back later.";
        }
        StringBuilder sb = new StringBuilder("Today's deals near you:");
        int shown = 0;
        for (ProductDto p : deals) {
            if (shown++ >= 6) {
                break;
            }
            sb.append("\n").append(productLine(p));
        }
        return sb.toString();
    }

    public String products(String term, List<ProductDto> found) {
        if (found == null || found.isEmpty()) {
            return "I couldn't find \"" + term + "\" near you. Try another name, or tap an option:";
        }
        StringBuilder sb = new StringBuilder("Here's what I found for \"" + term + "\" near you:");
        int shown = 0;
        for (ProductDto p : found) {
            if (shown++ >= 5) {
                break;
            }
            sb.append("\n").append(productLine(p));
        }
        return sb.toString();
    }

    private String productLine(ProductDto p) {
        StringBuilder sb = new StringBuilder("- ").append(p.getName()).append(": ").append(money(p.getPrice()));
        if (p.getUnit() != null && !p.getUnit().isBlank()) {
            sb.append(" per ").append(p.getUnit());
        }
        if (p.getOriginalPrice() != null && p.getPrice() != null
                && p.getOriginalPrice().compareTo(p.getPrice()) > 0) {
            sb.append(" (was ").append(money(p.getOriginalPrice()));
            if (p.getDiscountPercent() != null && p.getDiscountPercent() > 0) {
                sb.append(", ").append(p.getDiscountPercent()).append("% off");
            }
            sb.append(")");
        }
        return sb.toString();
    }
}
