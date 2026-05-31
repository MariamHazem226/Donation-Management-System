package com.givinghands.givinghands.service.impl;

import com.givinghands.givinghands.entity.Campaign;
import com.givinghands.givinghands.entity.User;
import com.givinghands.givinghands.notification.EmailTemplateName;
import com.givinghands.givinghands.notification.NotificationEventType;
import com.givinghands.givinghands.notification.NotificationRecipient;
import com.givinghands.givinghands.notification.NotificationRequest;
import com.givinghands.givinghands.notification.TemplateContext;
import com.givinghands.givinghands.repository.CampaignRepository;
import com.givinghands.givinghands.repository.UserRepository;
import com.givinghands.givinghands.service.AdminNotificationService;
import com.givinghands.givinghands.service.NotificationService;
import com.givinghands.givinghands.util.EmailTemplateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AdminNotificationServiceImpl.class);

    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final NotificationService notificationService;

    public AdminNotificationServiceImpl(UserRepository userRepository,
                                        CampaignRepository campaignRepository,
                                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void notifyCampaignGoalReached(Long campaignId) {
        Campaign campaign = loadCampaign(campaignId);
        List<User> admins = findAdmins();
        if (admins.isEmpty()) {
            log.warn("No admin users found for campaign goal-reached notification");
            return;
        }

        for (User admin : admins) {
            NotificationRecipient recipient = adminRecipient(admin);
            if (recipient == null) continue;

            TemplateContext ctx = EmailTemplateUtil.baseContext()
                    .put("title", "Campaign Goal Reached")
                    .put("subtitle", "A campaign has reached its fundraising goal")
                    .put("campaignTitle", campaign.getTitle())
                    .put("goalAmount", campaign.getGoalAmount())
                    .put("currentAmount", campaign.getCurrentAmount())
                    .put("deadline", campaign.getDeadline());

            notificationService.sendEmail(new NotificationRequest(
                    NotificationEventType.ADMIN_CAMPAIGN_GOAL_REACHED,
                    EmailTemplateName.ADMIN_CAMPAIGN_GOAL_REACHED,
                    recipient,
                    ctx
            ));
        }
        log.info("Queued admin goal-reached emails for {} admin(s), campaignId={}", admins.size(), campaignId);
    }

    @Override
    public void notifyNewCampaignRequest(Long campaignId) {
        Campaign campaign = loadCampaign(campaignId);
        List<User> admins = findAdmins();
        if (admins.isEmpty()) {
            log.warn("No admin users found for new campaign request notification");
            return;
        }

        User org = campaign.getCreator();
        String orgName = org != null ? org.getName() : "Unknown organization";
        String orgEmail = org != null ? org.getEmail() : "";

        for (User admin : admins) {
            NotificationRecipient recipient = adminRecipient(admin);
            if (recipient == null) continue;

            TemplateContext ctx = EmailTemplateUtil.baseContext()
                    .put("campaignTitle", campaign.getTitle())
                    .put("orgName", orgName)
                    .put("orgEmail", orgEmail)
                    .put("goalAmount", campaign.getGoalAmount())
                    .put("deadline", campaign.getDeadline())
                    .put("status", campaign.getStatus());

            notificationService.sendEmail(new NotificationRequest(
                    NotificationEventType.ADMIN_NEW_CAMPAIGN_REQUEST,
                    EmailTemplateName.ADMIN_NEW_CAMPAIGN_REQUEST,
                    recipient,
                    ctx
            ));
        }
        log.info("Queued admin new-campaign-request emails for {} admin(s), campaignId={}", admins.size(), campaignId);
    }

    @Override
    public void notifyNewOrganizationRegistered(Long organizationId) {
        User org = userRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));

        List<User> admins = findAdmins();
        if (admins.isEmpty()) {
            log.warn("No admin users found for new organization registration notification");
            return;
        }

        for (User admin : admins) {
            NotificationRecipient recipient = adminRecipient(admin);
            if (recipient == null) continue;

            TemplateContext ctx = EmailTemplateUtil.baseContext()
                    .put("orgName", org.getName())
                    .put("orgEmail", org.getEmail());

            notificationService.sendEmail(new NotificationRequest(
                    NotificationEventType.ADMIN_NEW_ORG_REGISTERED,
                    EmailTemplateName.ADMIN_NEW_ORG_REGISTERED,
                    recipient,
                    ctx
            ));
        }
        log.info("Queued admin new-org-registered emails for {} admin(s), organizationId={}",
                admins.size(), organizationId);
    }

    private Campaign loadCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + campaignId));
    }

    private List<User> findAdmins() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "ADMIN".equalsIgnoreCase(u.getRole().trim()))
                .toList();
    }

    private NotificationRecipient adminRecipient(User admin) {
        if (admin.getEmail() == null || admin.getEmail().isBlank()) {
            return null;
        }
        return new NotificationRecipient(admin.getEmail(), admin.getName());
    }
}
