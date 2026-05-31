package com.givinghands.givinghands.service.impl;

import com.givinghands.givinghands.entity.Campaign;
import com.givinghands.givinghands.entity.NewsletterSubscriber;
import com.givinghands.givinghands.notification.EmailTemplateName;
import com.givinghands.givinghands.notification.NotificationEventType;
import com.givinghands.givinghands.notification.NotificationRecipient;
import com.givinghands.givinghands.notification.NotificationRequest;
import com.givinghands.givinghands.notification.TemplateContext;
import com.givinghands.givinghands.repository.CampaignRepository;
import com.givinghands.givinghands.service.NewsletterNotificationService;
import com.givinghands.givinghands.service.NewsletterService;
import com.givinghands.givinghands.service.NotificationService;
import com.givinghands.givinghands.util.EmailTemplateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Year;

@Service
public class NewsletterNotificationServiceImpl implements NewsletterNotificationService {

    private static final Logger log = LoggerFactory.getLogger(NewsletterNotificationServiceImpl.class);

    private final NewsletterService newsletterService;
    private final CampaignRepository campaignRepository;
    private final NotificationService notificationService;

    public NewsletterNotificationServiceImpl(NewsletterService newsletterService,
                                             CampaignRepository campaignRepository,
                                             NotificationService notificationService) {
        this.newsletterService = newsletterService;
        this.campaignRepository = campaignRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void notifyCampaignApprovedActive(Long campaignId) {
        Campaign campaign = loadCampaign(campaignId);
        var subscribers = newsletterService.getActiveSubscribers();
        if (subscribers.isEmpty()) {
            log.debug("No active newsletter subscribers for campaign approved notification");
            return;
        }

        for (NewsletterSubscriber sub : subscribers) {
            if (sub.getEmail() == null || sub.getEmail().isBlank()) continue;

            TemplateContext ctx = EmailTemplateUtil.baseContext()
                    .put("title", "New Active Campaign")
                    .put("subtitle", "A campaign you may care about is now live")
                    .put("campaignTitle", campaign.getTitle())
                    .put("goalAmount", campaign.getGoalAmount())
                    .put("currentAmount", campaign.getCurrentAmount())
                    .put("deadline", campaign.getDeadline());

            notificationService.sendEmail(new NotificationRequest(
                    NotificationEventType.NEWSLETTER_CAMPAIGN_APPROVED_ACTIVE,
                    EmailTemplateName.CAMPAIGN_APPROVED_ACTIVE,
                    new NotificationRecipient(sub.getEmail(), sub.getName()),
                    ctx
            ));
        }
        log.info("Queued campaign-approved-active emails for {} subscriber(s), campaignId={}",
                subscribers.size(), campaignId);
    }

    @Override
    public void notifyCampaignGoalReached(Long campaignId) {
        Campaign campaign = loadCampaign(campaignId);
        var subscribers = newsletterService.getActiveSubscribers();
        if (subscribers.isEmpty()) {
            return;
        }

        for (NewsletterSubscriber sub : subscribers) {
            if (sub.getEmail() == null || sub.getEmail().isBlank()) continue;

            TemplateContext ctx = EmailTemplateUtil.baseContext()
                    .put("title", "Goal Reached")
                    .put("subtitle", "A campaign you follow reached its fundraising goal")
                    .put("campaignTitle", campaign.getTitle())
                    .put("goalAmount", campaign.getGoalAmount())
                    .put("currentAmount", campaign.getCurrentAmount())
                    .put("deadline", campaign.getDeadline());

            notificationService.sendEmail(new NotificationRequest(
                    NotificationEventType.NEWSLETTER_CAMPAIGN_GOAL_REACHED,
                    EmailTemplateName.CAMPAIGN_GOAL_REACHED,
                    new NotificationRecipient(sub.getEmail(), sub.getName()),
                    ctx
            ));
        }
        log.info("Queued goal-reached emails for {} subscriber(s), campaignId={}",
                subscribers.size(), campaignId);
    }

    @Override
    public void notifyVolunteerOpportunityAvailable(Long campaignId) {
        Campaign campaign = loadCampaign(campaignId);
        if (!campaign.isAllowVolunteers()) {
            log.debug("Skipping volunteer opportunity email — volunteering disabled for campaign {}", campaignId);
            return;
        }

        var subscribers = newsletterService.getActiveSubscribers();
        if (subscribers.isEmpty()) {
            return;
        }

        for (NewsletterSubscriber sub : subscribers) {
            if (sub.getEmail() == null || sub.getEmail().isBlank()) continue;

            TemplateContext ctx = EmailTemplateUtil.baseContext()
                    .put("title", "New Volunteer Opportunity")
                    .put("subtitle", "A campaign is accepting volunteer applications")
                    .put("campaignTitle", campaign.getTitle());

            notificationService.sendEmail(new NotificationRequest(
                    NotificationEventType.NEWSLETTER_NEW_VOLUNTEER_OPPORTUNITIES_AVAILABLE,
                    EmailTemplateName.NEW_VOLUNTEER_OPPORTUNITY,
                    new NotificationRecipient(sub.getEmail(), sub.getName()),
                    ctx
            ));
        }
        log.info("Queued volunteer-opportunity emails for {} subscriber(s), campaignId={}",
                subscribers.size(), campaignId);
    }

    private Campaign loadCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + campaignId));
    }
}
