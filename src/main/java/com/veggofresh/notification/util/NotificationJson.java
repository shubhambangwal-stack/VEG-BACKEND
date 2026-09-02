package com.veggofresh.notification.util;

/**
 * Tiny JSON-string builder helpers for {@code data} payloads on notifications.
 * The notification engine treats {@code data} as opaque, but callers that stuff
 * user-provided text (comments, shop names …) into it must escape it so client
 * parsers never see broken JSON.
 */
public final class NotificationJson {

    private NotificationJson() {
    }

    /**
     * Escapes a value for safe embedding inside a JSON string literal.
     */
    public static String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Convenience for the common {@code "key": "value"} string pair with a
     * default value of {@code null}.
     */
    public static String str(String value) {
        return "\"" + esc(value) + "\"";
    }
}