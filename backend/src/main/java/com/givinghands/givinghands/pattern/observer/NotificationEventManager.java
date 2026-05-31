package com.givinghands.givinghands.pattern.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Subject / Hub in the Observer Pattern.
 * Maintains observers and broadcasts events to all subscribers.
 */
@Component
public class NotificationEventManager {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventManager.class);

    private final List<NotificationListener> listeners = new ArrayList<>();

    public void subscribe(NotificationListener listener) {
        if (listener == null) {
            return;
        }
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            log.debug("Observer subscribed: {}", listener.getClass().getSimpleName());
        }
    }

    public void unsubscribe(NotificationListener listener) {
        if (listener == null) {
            return;
        }
        listeners.remove(listener);
        log.debug("Observer unsubscribed: {}", listener.getClass().getSimpleName());
    }

    public void notify(String eventType, Object data) {
        if (eventType == null || eventType.isBlank()) {
            log.warn("Ignoring notification with blank eventType");
            return;
        }
        log.debug("Notifying {} observer(s). eventType={}", listeners.size(), eventType);
        for (NotificationListener listener : listeners) {
            listener.update(eventType, data);
        }
    }

    /** Read-only view for diagnostics / tests. */
    public List<NotificationListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }
}
