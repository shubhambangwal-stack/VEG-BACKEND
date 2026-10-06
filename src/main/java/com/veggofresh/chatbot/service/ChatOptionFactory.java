package com.veggofresh.chatbot.service;

import com.veggofresh.chatbot.dto.response.ChatContextDto;
import com.veggofresh.chatbot.dto.response.ChatOptionDto;
import com.veggofresh.chatbot.dto.response.ChatOrderCardDto;
import com.veggofresh.customer.dto.response.OrderResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the tappable buttons, order cards and "talking about" context.
 * Which buttons appear depends on the order's real status -- e.g. "Delivery partner"
 * is only offered once there is something to say about one.
 */
@Component
@RequiredArgsConstructor
public class ChatOptionFactory {

    private final ReplyBuilder replies;

    private static ChatOptionDto opt(String id, String label) {
        return ChatOptionDto.builder().id(id).label(label).build();
    }

    /** Buttons shown right after the greeting. */
    public List<ChatOptionDto> greetingMenu(boolean hasOrders) {
        List<ChatOptionDto> out = new ArrayList<>();
        if (hasOrders) {
            out.add(opt("DETAILS", "Yes, show details"));
            out.add(opt("ORDERS", "My orders"));
        }
        out.add(opt("DEALS", "Today's deals"));
        out.add(opt("SEARCH", "Search a product"));
        out.add(opt("CART", "My cart"));
        return out;
    }

    /** Generic menu, used when no order is being discussed. */
    public List<ChatOptionDto> mainMenu() {
        List<ChatOptionDto> out = new ArrayList<>();
        out.add(opt("ORDERS", "My orders"));
        out.add(opt("DEALS", "Today's deals"));
        out.add(opt("SEARCH", "Search a product"));
        out.add(opt("CART", "My cart"));
        out.add(opt("END", "No, that's all"));
        return out;
    }

    /** Buttons for one order, chosen by its current status. */
    public List<ChatOptionDto> forOrder(OrderResponseDto o) {
        String id = o.getId().toString();
        String status = o.getStatus() == null ? "" : o.getStatus().toUpperCase(Locale.ROOT);
        List<ChatOptionDto> out = new ArrayList<>();

        switch (status) {
            case "PLACED":
                out.add(opt("TRACK:" + id, "Track order"));
                out.add(opt("PRICE:" + id, "Price details"));
                out.add(opt("ITEMS:" + id, "Items"));
                out.add(opt("SLOT:" + id, "Address & slot"));
                break;
            case "CONFIRMED":
            case "PREPARING":
                out.add(opt("TRACK:" + id, "Track order"));
                out.add(opt("PRICE:" + id, "Price details"));
                out.add(opt("ITEMS:" + id, "Items"));
                out.add(opt("SHOP:" + id, "Shop details"));
                out.add(opt("SLOT:" + id, "Address & slot"));
                break;
            case "READY_FOR_PICKUP":
                out.add(opt("TRACK:" + id, "Track order"));
                out.add(opt("PRICE:" + id, "Price details"));
                out.add(opt("ITEMS:" + id, "Items"));
                out.add(opt("SHOP:" + id, "Shop details"));
                if (ReplyBuilder.hasAgent(o)) {
                    out.add(opt("PARTNER:" + id, "Delivery partner"));
                }
                out.add(opt("SLOT:" + id, "Address & slot"));
                break;
            case "OUT_FOR_DELIVERY":
                out.add(opt("TRACK:" + id, "Track order"));
                out.add(opt("PRICE:" + id, "Price details"));
                out.add(opt("ITEMS:" + id, "Items"));
                out.add(opt("PARTNER:" + id, "Delivery partner"));
                out.add(opt("SLOT:" + id, "Address & slot"));
                out.add(opt("INVOICE:" + id, "Invoice"));
                break;
            case "DELIVERED":
                out.add(opt("PRICE:" + id, "Price details"));
                out.add(opt("ITEMS:" + id, "Items"));
                out.add(opt("PARTNER:" + id, "Delivery partner"));
                out.add(opt("INVOICE:" + id, "Invoice"));
                out.add(opt("SLOT:" + id, "Address & slot"));
                break;
            case "CANCELLED":
                out.add(opt("PRICE:" + id, "Price details"));
                out.add(opt("ITEMS:" + id, "Items"));
                break;
            default:
                out.add(opt("TRACK:" + id, "Track order"));
                out.add(opt("PRICE:" + id, "Price details"));
                out.add(opt("ITEMS:" + id, "Items"));
                break;
        }
        out.add(opt("ORDERS", "Another order"));
        out.add(opt("END", "No, that's all"));
        return out;
    }

    /** A tappable order card. */
    public ChatOrderCardDto card(OrderResponseDto o) {
        return ChatOrderCardDto.builder()
                .optionId("ORDER:" + o.getId())
                .orderId(o.getId())
                .orderNumber(o.getOrderNumber())
                .status(o.getStatus())
                .statusLabel(replies.statusLabel(o))
                .total(o.getTotalAmount())
                .itemCount(o.getItemCount())
                .placedAt(o.getCreatedAt())
                .build();
    }

    /** The "Talking about ..." context for an order. */
    public ChatContextDto context(OrderResponseDto o) {
        return ChatContextDto.builder()
                .orderId(o.getId())
                .orderNumber(o.getOrderNumber())
                .statusLabel(replies.statusLabel(o))
                .build();
    }
}
