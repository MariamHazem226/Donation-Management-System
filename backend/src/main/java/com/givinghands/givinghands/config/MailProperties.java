package com.givinghands.givinghands.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SMTP configuration guard — does not send mail, only reports whether Gmail SMTP is configured.
 */
@Component
public class MailProperties {

    @Value("${givinghands.mail.enabled:true}")
    private boolean enabled;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${givinghands.notification.from-name:GivingHands}")
    private String fromName;

    public boolean isEnabled() {
        return enabled;
    }

    public String getFromName() {
        return fromName;
    }

    public String getUsername() {
        return username;
    }

    public boolean isConfigured() {
        if (!enabled) {
            return false;
        }
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return false;
        }
        String u = username.toLowerCase();
        String p = password.toLowerCase();
        return !u.contains("your@gmail") && !p.contains("your-app") && !p.contains("password");
    }

    public String configurationHint() {
        if (!enabled) {
            return "Mail is disabled (givinghands.mail.enabled=false).";
        }
        if (!isConfigured()) {
            return "Set MAIL_USERNAME and MAIL_PASSWORD environment variables (Gmail App Password). "
                    + "See backend/application.properties.example";
        }
        return "SMTP configured for " + username;
    }
}
