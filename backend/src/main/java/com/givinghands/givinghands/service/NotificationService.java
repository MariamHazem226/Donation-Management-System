package com.givinghands.givinghands.service;

import com.givinghands.givinghands.notification.NotificationRequest;

/**
 * Infrastructure contract for outbound notifications (email channel).
 * Business-specific recipient resolution and event handlers are added in a later layer.
 */
public interface NotificationService {

    /**
     * Sends a templated HTML email asynchronously.
     */
    void sendEmail(NotificationRequest request);

    /**
     * Sends a raw HTML email asynchronously (no Thymeleaf template).
     */
    void sendHtmlEmail(String to, String subject, String htmlBody);
}
