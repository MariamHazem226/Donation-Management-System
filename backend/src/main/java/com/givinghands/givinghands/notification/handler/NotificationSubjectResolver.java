package com.givinghands.givinghands.notification.handler;

import com.givinghands.givinghands.notification.EmailTemplateName;
import com.givinghands.givinghands.notification.NotificationRequest;
import org.springframework.stereotype.Component;

/**
 * Central place for default email subjects per template.
 */
@Component
public class NotificationSubjectResolver {

    public String resolve(NotificationRequest request) {
        return resolve(request.template());
    }

    public String resolve(EmailTemplateName template) {
        return switch (template) {
            case CAMPAIGN_APPROVED_ACTIVE, ORG_CAMPAIGN_APPROVED -> "GivingHands: Your campaign is approved";
            case CAMPAIGN_GOAL_REACHED, ADMIN_CAMPAIGN_GOAL_REACHED -> "GivingHands: Goal reached";
            case NEW_VOLUNTEER_OPPORTUNITY -> "GivingHands: New volunteer opportunity available";
            case ORG_CAMPAIGN_REJECTED -> "GivingHands: Campaign rejected";
            case ORG_REGISTRATION_WELCOME -> "GivingHands: Welcome to GivingHands";
            case ORG_WELCOME_APPROVED -> "GivingHands: Your organization is approved";
            case ORG_VOLUNTEER_APPLIED -> "GivingHands: New volunteer application";
            case USER_DONATION_SUBMITTED -> "GivingHands: Donation received";
            case USER_VOLUNTEER_APPROVED -> "GivingHands: Volunteer application approved";
            case USER_VOLUNTEER_REJECTED -> "GivingHands: Volunteer application rejected";
            case ADMIN_NEW_ORG_REGISTERED -> "GivingHands: New organization registered";
            case ADMIN_NEW_CAMPAIGN_REQUEST -> "GivingHands: New campaign pending approval";
        };
    }
}
