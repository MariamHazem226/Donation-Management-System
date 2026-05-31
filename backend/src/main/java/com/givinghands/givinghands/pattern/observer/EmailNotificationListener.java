package com.givinghands.givinghands.pattern.observer;

import com.givinghands.givinghands.notification.NotificationEvents;
import com.givinghands.givinghands.service.AdminNotificationService;
import com.givinghands.givinghands.service.NewsletterNotificationService;
import com.givinghands.givinghands.service.OrganizationNotificationService;
import com.givinghands.givinghands.service.UserNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Observer that routes events to notification services,
 * which delegate delivery to {@link com.givinghands.givinghands.service.NotificationService}.
 */
@Component
public class EmailNotificationListener implements NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationListener.class);

    private final NewsletterNotificationService newsletterNotificationService;
    private final OrganizationNotificationService organizationNotificationService;
    private final UserNotificationService userNotificationService;
    private final AdminNotificationService adminNotificationService;

    public EmailNotificationListener(NewsletterNotificationService newsletterNotificationService,
                                     OrganizationNotificationService organizationNotificationService,
                                     UserNotificationService userNotificationService,
                                     AdminNotificationService adminNotificationService) {
        this.newsletterNotificationService = newsletterNotificationService;
        this.organizationNotificationService = organizationNotificationService;
        this.userNotificationService = userNotificationService;
        this.adminNotificationService = adminNotificationService;
    }

    @Override
    public void update(String eventType, Object data) {
        if (eventType == null || eventType.isBlank()) {
            return;
        }

        switch (eventType) {
            case NotificationEvents.NEWSLETTER_CAMPAIGN_APPROVED_ACTIVE -> {
                Long campaignId = extractLong(data, NotificationEvents.CAMPAIGN_ID);
                if (campaignId != null) newsletterNotificationService.notifyCampaignApprovedActive(campaignId);
            }
            case NotificationEvents.NEWSLETTER_CAMPAIGN_GOAL_REACHED -> {
                Long campaignId = extractLong(data, NotificationEvents.CAMPAIGN_ID);
                if (campaignId != null) newsletterNotificationService.notifyCampaignGoalReached(campaignId);
            }
            case NotificationEvents.NEWSLETTER_VOLUNTEER_OPPORTUNITY -> {
                Long campaignId = extractLong(data, NotificationEvents.CAMPAIGN_ID);
                if (campaignId != null) newsletterNotificationService.notifyVolunteerOpportunityAvailable(campaignId);
            }
            case NotificationEvents.ORG_REGISTRATION_WELCOME -> {
                Long organizationId = extractLong(data, NotificationEvents.ORGANIZATION_ID);
                if (organizationId != null) organizationNotificationService.notifyOrganizationRegistered(organizationId);
            }
            case NotificationEvents.ORG_WELCOME_APPROVED -> {
                Long organizationId = extractLong(data, NotificationEvents.ORGANIZATION_ID);
                if (organizationId != null) organizationNotificationService.notifyOrganizationWelcomeApproved(organizationId);
            }
            case NotificationEvents.ORG_VOLUNTEER_APPLIED -> {
                Long volunteerId = extractLong(data, NotificationEvents.VOLUNTEER_ID);
                if (volunteerId != null) organizationNotificationService.notifyVolunteerApplied(volunteerId);
            }
            case NotificationEvents.ORG_CAMPAIGN_APPROVED -> {
                Long campaignId = extractLong(data, NotificationEvents.CAMPAIGN_ID);
                if (campaignId != null) organizationNotificationService.notifyCampaignApproved(campaignId);
            }
            case NotificationEvents.ORG_CAMPAIGN_REJECTED -> {
                Long campaignId = extractLong(data, NotificationEvents.CAMPAIGN_ID);
                if (campaignId != null) organizationNotificationService.notifyCampaignRejected(campaignId);
            }
            case NotificationEvents.USER_DONATION_SUBMITTED -> {
                Long donationId = extractLong(data, NotificationEvents.DONATION_ID);
                if (donationId != null) userNotificationService.notifyDonationSubmitted(donationId);
            }
            case NotificationEvents.USER_VOLUNTEER_APPROVED -> {
                Long volunteerId = extractLong(data, NotificationEvents.VOLUNTEER_ID);
                if (volunteerId != null) userNotificationService.notifyVolunteerApproved(volunteerId);
            }
            case NotificationEvents.USER_VOLUNTEER_REJECTED -> {
                Long volunteerId = extractLong(data, NotificationEvents.VOLUNTEER_ID);
                if (volunteerId != null) userNotificationService.notifyVolunteerRejected(volunteerId);
            }
            case NotificationEvents.ADMIN_CAMPAIGN_GOAL_REACHED -> {
                Long campaignId = extractLong(data, NotificationEvents.CAMPAIGN_ID);
                if (campaignId != null) adminNotificationService.notifyCampaignGoalReached(campaignId);
            }
            case NotificationEvents.ADMIN_NEW_CAMPAIGN_REQUEST -> {
                Long campaignId = extractLong(data, NotificationEvents.CAMPAIGN_ID);
                if (campaignId != null) adminNotificationService.notifyNewCampaignRequest(campaignId);
            }
            case NotificationEvents.ADMIN_NEW_ORG_REGISTERED -> {
                Long organizationId = extractLong(data, NotificationEvents.ORGANIZATION_ID);
                if (organizationId != null) adminNotificationService.notifyNewOrganizationRegistered(organizationId);
            }
            default -> log.debug("Email listener ignoring unhandled eventType={}", eventType);
        }
    }

    private Long extractLong(Object data, String key) {
        if (!(data instanceof Map<?, ?> map)) {
            return null;
        }
        Object value = map.get(key);
        if (value == null && NotificationEvents.CAMPAIGN_ID.equals(key)) {
            value = map.get("campaignId");
        }
        if (value == null && NotificationEvents.VOLUNTEER_ID.equals(key)) {
            value = map.get("volunteerId");
        }
        if (value == null && NotificationEvents.DONATION_ID.equals(key)) {
            value = map.get("donationId");
        }
        if (value == null && NotificationEvents.ORGANIZATION_ID.equals(key)) {
            value = map.get("organizationId");
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
