package com.givinghands.givinghands.pattern.observer;

/**
 * Observer interface (GoF Observer Pattern).
 * Implementations react to notification events published by {@link NotificationEventManager}.
 */
public interface NotificationListener {

    /**
     * Called when the subject publishes an event.
     *
     * @param eventType domain event identifier (e.g. {@code "campaign_approved"})
     * @param data      optional payload (maps, DTOs, ids, etc.)
     */
    void update(String eventType, Object data);
}
