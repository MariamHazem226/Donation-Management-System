package com.givinghands.givinghands.config;

import com.givinghands.givinghands.pattern.observer.EmailNotificationListener;
import com.givinghands.givinghands.pattern.observer.LoggingNotificationListener;
import com.givinghands.givinghands.pattern.observer.NotificationEventManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Observer Pattern hub with its default listeners.
 */
@Configuration
public class NotificationObserverConfig {

    @Bean
    CommandLineRunner registerNotificationObservers(
            NotificationEventManager eventManager,
            LoggingNotificationListener loggingListener,
            EmailNotificationListener emailListener) {
        return args -> {
            eventManager.subscribe(loggingListener);
            eventManager.subscribe(emailListener);
        };
    }
}
