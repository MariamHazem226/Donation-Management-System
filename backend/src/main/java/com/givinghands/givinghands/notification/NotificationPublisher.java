package com.givinghands.givinghands.notification;

import com.givinghands.givinghands.pattern.observer.NotificationEventManager;
import org.springframework.stereotype.Component;

/**
 * Application entry point for publishing notification events.
 * Controllers and services should use this (or {@link com.givinghands.givinghands.notification.handler.NotificationDispatcher})
 * instead of calling {@link com.givinghands.givinghands.service.NotificationService} directly.
 */
@Component
public class NotificationPublisher {

    private final NotificationEventManager eventManager;

    public NotificationPublisher(NotificationEventManager eventManager) {
        this.eventManager = eventManager;
    }

    public void publish(String eventType, Object data) {
        eventManager.notify(eventType, data);
    }
}
