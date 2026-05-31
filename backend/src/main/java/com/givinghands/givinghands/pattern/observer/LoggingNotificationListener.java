package com.givinghands.givinghands.pattern.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Observer that will log notification events.
 * Stub only — business logging will be added in a later phase.
 */
@Component
public class LoggingNotificationListener implements NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationListener.class);

    @Override
    public void update(String eventType, Object data) {
        // Foundation stub — no business logic yet
        log.debug("[LoggingNotificationListener] eventType={}, data={}", eventType, data);
    }
}
