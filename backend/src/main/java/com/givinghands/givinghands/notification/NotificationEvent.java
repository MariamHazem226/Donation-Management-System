package com.givinghands.givinghands.notification;

import java.util.Map;

public record NotificationEvent(
        NotificationEventType type,
        Map<String, Object> payload
) {

    public Object get(String key) {
        return payload != null ? payload.get(key) : null;
    }
}

