package com.givinghands.givinghands.service.impl;

import com.givinghands.givinghands.entity.Campaign;
import com.givinghands.givinghands.entity.Donation;
import com.givinghands.givinghands.entity.User;
import com.givinghands.givinghands.entity.Volunteer;
import com.givinghands.givinghands.notification.EmailTemplateName;
import com.givinghands.givinghands.notification.NotificationEventType;
import com.givinghands.givinghands.notification.NotificationRecipient;
import com.givinghands.givinghands.notification.NotificationRequest;
import com.givinghands.givinghands.notification.TemplateContext;
import com.givinghands.givinghands.repository.CampaignRepository;
import com.givinghands.givinghands.repository.DonationRepository;
import com.givinghands.givinghands.repository.UserRepository;
import com.givinghands.givinghands.repository.VolunteerRepository;
import com.givinghands.givinghands.service.NotificationService;
import com.givinghands.givinghands.service.UserNotificationService;
import com.givinghands.givinghands.util.EmailTemplateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserNotificationServiceImpl implements UserNotificationService {

    private static final Logger log = LoggerFactory.getLogger(UserNotificationServiceImpl.class);

    private final DonationRepository donationRepository;
    private final VolunteerRepository volunteerRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final NotificationService notificationService;

    public UserNotificationServiceImpl(DonationRepository donationRepository,
                                       VolunteerRepository volunteerRepository,
                                       UserRepository userRepository,
                                       CampaignRepository campaignRepository,
                                       NotificationService notificationService) {
        this.donationRepository = donationRepository;
        this.volunteerRepository = volunteerRepository;
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void notifyDonationSubmitted(Long donationId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new RuntimeException("Donation not found: " + donationId));

        User user = userRepository.findById(donation.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + donation.getUserId()));

        NotificationRecipient recipient = userRecipient(user);
        if (recipient == null) {
            log.warn("Skipping donation-submitted email — user email missing for donation {}", donationId);
            return;
        }

        Campaign campaign = campaignRepository.findById(donation.getCampaignId()).orElse(null);

        TemplateContext ctx = EmailTemplateUtil.baseContext()
                .put("campaignTitle", campaign != null ? campaign.getTitle() : "Campaign")
                .put("amount", donation.getAmount())
                .put("date", donation.getDate());

        notificationService.sendEmail(new NotificationRequest(
                NotificationEventType.USER_DONATION_SUBMITTED,
                EmailTemplateName.USER_DONATION_SUBMITTED,
                recipient,
                ctx
        ));
        log.info("Queued user donation-submitted email. donationId={}, to={}", donationId, recipient.email());
    }

    @Override
    public void notifyVolunteerApproved(Long volunteerId) {
        sendVolunteerStatusEmail(volunteerId, NotificationEventType.USER_VOLUNTEER_APPROVED,
                EmailTemplateName.USER_VOLUNTEER_APPROVED);
    }

    @Override
    public void notifyVolunteerRejected(Long volunteerId) {
        sendVolunteerStatusEmail(volunteerId, NotificationEventType.USER_VOLUNTEER_REJECTED,
                EmailTemplateName.USER_VOLUNTEER_REJECTED);
    }

    private void sendVolunteerStatusEmail(Long volunteerId,
                                          NotificationEventType eventType,
                                          EmailTemplateName template) {
        Volunteer volunteer = volunteerRepository.findById(volunteerId)
                .orElseThrow(() -> new RuntimeException("Volunteer not found: " + volunteerId));

        User user = volunteer.getUser();
        if (user == null) {
            log.warn("Skipping volunteer status email — user missing for volunteer {}", volunteerId);
            return;
        }

        NotificationRecipient recipient = userRecipient(user);
        if (recipient == null) {
            log.warn("Skipping volunteer status email — user email missing for volunteer {}", volunteerId);
            return;
        }

        Campaign campaign = volunteer.getCampaign();
        TemplateContext ctx = EmailTemplateUtil.baseContext()
                .put("campaignTitle", campaign != null ? campaign.getTitle() : "Campaign");

        notificationService.sendEmail(new NotificationRequest(
                eventType,
                template,
                recipient,
                ctx
        ));
        log.info("Queued user volunteer email. volunteerId={}, template={}, to={}",
                volunteerId, template, recipient.email());
    }

    private NotificationRecipient userRecipient(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return null;
        }
        return new NotificationRecipient(user.getEmail(), user.getName());
    }
}
