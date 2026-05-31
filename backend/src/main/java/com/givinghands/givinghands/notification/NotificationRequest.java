package com.givinghands.givinghands.notification;

public record NotificationRequest(
        NotificationEventType eventType,
        EmailTemplateName template,
        NotificationRecipient recipient,
        TemplateContext context
) {}

