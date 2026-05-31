package com.givinghands.givinghands.service.impl;

import com.givinghands.givinghands.entity.Campaign;
import com.givinghands.givinghands.entity.User;
import com.givinghands.givinghands.entity.Volunteer;
import com.givinghands.givinghands.notification.EmailTemplateName;
import com.givinghands.givinghands.notification.NotificationEventType;
import com.givinghands.givinghands.notification.NotificationRecipient;
import com.givinghands.givinghands.notification.NotificationRequest;
import com.givinghands.givinghands.notification.TemplateContext;
import com.givinghands.givinghands.repository.CampaignRepository;
import com.givinghands.givinghands.repository.UserRepository;
import com.givinghands.givinghands.repository.VolunteerRepository;
import com.givinghands.givinghands.service.NotificationService;
import com.givinghands.givinghands.service.OrganizationNotificationService;
import com.givinghands.givinghands.util.EmailTemplateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrganizationNotificationServiceImpl implements OrganizationNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationNotificationServiceImpl.class);

    private final VolunteerRepository volunteerRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public OrganizationNotificationServiceImpl(VolunteerRepository volunteerRepository,
                                               CampaignRepository campaignRepository,
                                               UserRepository userRepository,
                                               NotificationService notificationService) {
        this.volunteerRepository = volunteerRepository;
        this.campaignRepository = campaignRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void notifyOrganizationRegistered(Long organizationId) {
        User org = loadOrganization(organizationId);
        NotificationRecipient recipient = orgRecipient(org);
        if (recipient == null) {
            log.warn("Skipping org welcome email — email missing for organization {}", organizationId);
            return;
        }

        TemplateContext ctx = EmailTemplateUtil.baseContext()
                .put("orgName", org.getName())
                .put("message", "Thank you for registering your organization on GivingHands.");

        notificationService.sendEmail(new NotificationRequest(
                NotificationEventType.ORG_REGISTRATION_WELCOME,
                EmailTemplateName.ORG_REGISTRATION_WELCOME,
                recipient,
                ctx
        ));
        log.info("Queued org registration welcome email. organizationId={}, to={}", organizationId, recipient.email());
    }

    @Override
    public void notifyOrganizationWelcomeApproved(Long organizationId) {
        User org = loadOrganization(organizationId);
        NotificationRecipient recipient = orgRecipient(org);
        if (recipient == null) {
            log.warn("Skipping org approval email — email missing for organization {}", organizationId);
            return;
        }

        TemplateContext ctx = EmailTemplateUtil.baseContext()
                .put("orgName", org.getName())
                .put("message", "Your organization account has been approved. You can sign in and start using the platform.");

        notificationService.sendEmail(new NotificationRequest(
                NotificationEventType.ORG_WELCOME_APPROVED,
                EmailTemplateName.ORG_WELCOME_APPROVED,
                recipient,
                ctx
        ));
        log.info("Queued org approval confirmation email. organizationId={}, to={}", organizationId, recipient.email());
    }

    @Override
    public void notifyVolunteerApplied(Long volunteerId) {
        Volunteer volunteer = volunteerRepository.findById(volunteerId)
                .orElseThrow(() -> new RuntimeException("Volunteer not found: " + volunteerId));

        Campaign campaign = volunteer.getCampaign();
        if (campaign == null) {
            log.warn("Skipping org volunteer-applied email — campaign missing for volunteer {}", volunteerId);
            return;
        }

        NotificationRecipient recipient = orgRecipient(campaign);
        if (recipient == null) {
            log.warn("Skipping org volunteer-applied email — organization email missing for campaign {}", campaign.getId());
            return;
        }

        User volunteerUser = volunteer.getUser();
        TemplateContext ctx = EmailTemplateUtil.baseContext()
                .put("campaignTitle", campaign.getTitle())
                .put("volunteerName", volunteerUser != null ? volunteerUser.getName() : "A volunteer");

        notificationService.sendEmail(new NotificationRequest(
                NotificationEventType.ORG_VOLUNTEER_APPLIED,
                EmailTemplateName.ORG_VOLUNTEER_APPLIED,
                recipient,
                ctx
        ));
        log.info("Queued org volunteer-applied email. volunteerId={}, to={}", volunteerId, recipient.email());
    }

    @Override
    public void notifyCampaignApproved(Long campaignId) {
        Campaign campaign = loadCampaign(campaignId);
        NotificationRecipient recipient = orgRecipient(campaign);
        if (recipient == null) {
            log.warn("Skipping org campaign-approved email — organization email missing for campaign {}", campaignId);
            return;
        }

        TemplateContext ctx = EmailTemplateUtil.baseContext()
                .put("campaignTitle", campaign.getTitle())
                .put("orgName", recipient.name())
                .put("message", "Your campaign request has been approved and is now active.");

        notificationService.sendEmail(new NotificationRequest(
                NotificationEventType.ORG_CAMPAIGN_APPROVED,
                EmailTemplateName.ORG_CAMPAIGN_APPROVED,
                recipient,
                ctx
        ));
        log.info("Queued org campaign-approved email. campaignId={}, to={}", campaignId, recipient.email());
    }

    @Override
    public void notifyCampaignRejected(Long campaignId) {
        Campaign campaign = loadCampaign(campaignId);
        NotificationRecipient recipient = orgRecipient(campaign);
        if (recipient == null) {
            log.warn("Skipping org campaign-rejected email — organization email missing for campaign {}", campaignId);
            return;
        }

        TemplateContext ctx = EmailTemplateUtil.baseContext()
                .put("campaignTitle", campaign.getTitle())
                .put("orgName", recipient.name())
                .put("message", "Your campaign request has been rejected by the admin.");

        notificationService.sendEmail(new NotificationRequest(
                NotificationEventType.ORG_CAMPAIGN_REJECTED,
                EmailTemplateName.ORG_CAMPAIGN_REJECTED,
                recipient,
                ctx
        ));
        log.info("Queued org campaign-rejected email. campaignId={}, to={}", campaignId, recipient.email());
    }

    private Campaign loadCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + campaignId));
    }

    private User loadOrganization(Long organizationId) {
        return userRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));
    }

    private NotificationRecipient orgRecipient(User org) {
        if (org == null || org.getEmail() == null || org.getEmail().isBlank()) {
            return null;
        }
        return new NotificationRecipient(org.getEmail(), org.getName());
    }

    private NotificationRecipient orgRecipient(Campaign campaign) {
        return orgRecipient(campaign != null ? campaign.getCreator() : null);
    }
}
