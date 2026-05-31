package com.givinghands.givinghands.notification.handler;

import com.givinghands.givinghands.notification.NotificationEvent;
import com.givinghands.givinghands.notification.NotificationPublisher;
import com.givinghands.givinghands.notification.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Central facade for the notification pipeline.
 * Routes events through the Observer hub — does not call {@link com.givinghands.givinghands.service.NotificationService} directly.
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationPublisher notificationPublisher;

    public NotificationDispatcher(NotificationPublisher notificationPublisher) {
        this.notificationPublisher = notificationPublisher;
    }

    /**
     * Publishes a domain event to all registered observers.
     */
    public void publish(String eventType, Object data) {
        log.debug("Publishing notification event. eventType={}", eventType);
        notificationPublisher.publish(eventType, data);
    }

    /**
     * Legacy path — forwards structured events to the observer hub (no direct email delivery).
     */
    public void dispatch(NotificationEvent event) {
        if (event == null || event.type() == null) {
            log.warn("Ignoring null notification event");
            return;
        }
        publish(event.type().name(), event.payload());
    }

    /**
     * Legacy path — accepts a fully built request but only publishes to observers (no email yet).
     */
    public void dispatch(NotificationRequest request) {
        if (request == null) {
            log.warn("Ignoring null notification request");
            return;
        }
        publish(
                request.eventType() != null ? request.eventType().name() : "UNKNOWN",
                Map.of(
                        "template", request.template() != null ? request.template().name() : null,
                        "recipient", request.recipient(),
                        "context", request.context() != null ? request.context().asMap() : Map.of()
                )
        );
    }

    /**
     * Campaign observer bridge entry — publishes a generic payload for future listeners.
     */
    public void dispatchCampaignObserverEvent(String eventType, Long campaignId, String details) {
        publish(eventType, Map.of(
                "campaignId", campaignId != null ? campaignId : 0L,
                "details", details != null ? details : ""
        ));
    }
}
