package com.givinghands.givinghands.service.impl;

import com.givinghands.givinghands.config.MailProperties;
import com.givinghands.givinghands.notification.NotificationRequest;
import com.givinghands.givinghands.notification.handler.NotificationSubjectResolver;
import com.givinghands.givinghands.service.NotificationService;
import com.givinghands.givinghands.util.EmailTemplateUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Gmail SMTP email delivery via {@link JavaMailSender}, with async sending.
 * Invoked from observer listeners (e.g. {@link com.givinghands.givinghands.pattern.observer.EmailNotificationListener}).
 */
@Service
public class EmailService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailTemplateUtil emailTemplateUtil;
    private final NotificationSubjectResolver subjectResolver;
    private final MailProperties mailProperties;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender,
                        EmailTemplateUtil emailTemplateUtil,
                        NotificationSubjectResolver subjectResolver,
                        MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.emailTemplateUtil = emailTemplateUtil;
        this.subjectResolver = subjectResolver;
        this.mailProperties = mailProperties;
    }

    @Override
    @Async("notificationTaskExecutor")
    public void sendEmail(NotificationRequest request) {
        if (!mailProperties.isConfigured()) {
            log.error("[EmailService] Skipped — SMTP not configured. {}", mailProperties.configurationHint());
            return;
        }

        log.info("[EmailService] sendEmail on thread='{}' template={} to={}",
                Thread.currentThread().getName(),
                request.template(),
                request.recipient() != null ? request.recipient().email() : "null");

        try {
            String html = emailTemplateUtil.render(
                    request.template(),
                    request.context() != null ? request.context().asMap() : Collections.emptyMap()
            );
            sendMime(request.recipient().email(), subjectResolver.resolve(request), html);
            log.info("Email sent. template={}, to={}", request.template(), request.recipient().email());
        } catch (Exception e) {
            log.error("Failed to send templated email. template={}, to={}",
                    request.template(), request.recipient() != null ? request.recipient().email() : "null", e);
        }
    }

    @Override
    @Async("notificationTaskExecutor")
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (!mailProperties.isConfigured()) {
            log.error("[EmailService] Skipped HTML email — SMTP not configured. {}", mailProperties.configurationHint());
            return;
        }

        log.info("[EmailService] sendHtmlEmail on thread='{}' to={}", Thread.currentThread().getName(), to);
        try {
            sendMime(to, subject, htmlBody);
            log.info("HTML email sent. to={}", to);
        } catch (Exception e) {
            log.error("Failed to send HTML email. to={}", to, e);
        }
    }

    private void sendMime(String to, String subject, String html) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(mailProperties.getUsername());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
    }
}
