package com.givinghands.givinghands.notification;

/**
 * Observer event type constants for the notification hub.
 */
public final class NotificationEvents {

    private NotificationEvents() {}

    // Newsletter subscribers
    public static final String NEWSLETTER_CAMPAIGN_APPROVED_ACTIVE = "newsletter_campaign_approved_active";
    public static final String NEWSLETTER_CAMPAIGN_GOAL_REACHED = "newsletter_campaign_goal_reached";
    public static final String NEWSLETTER_VOLUNTEER_OPPORTUNITY = "newsletter_volunteer_opportunity";

    // Organization (campaign creator)
    public static final String ORG_REGISTRATION_WELCOME = "org_registration_welcome";
    public static final String ORG_WELCOME_APPROVED = "org_welcome_approved";
    public static final String ORG_VOLUNTEER_APPLIED = "org_volunteer_applied";
    public static final String ORG_CAMPAIGN_APPROVED = "org_campaign_approved";
    public static final String ORG_CAMPAIGN_REJECTED = "org_campaign_rejected";

    // User
    public static final String USER_DONATION_SUBMITTED = "user_donation_submitted";
    public static final String USER_VOLUNTEER_APPROVED = "user_volunteer_approved";
    public static final String USER_VOLUNTEER_REJECTED = "user_volunteer_rejected";

    // Admin
    public static final String ADMIN_CAMPAIGN_GOAL_REACHED = "admin_campaign_goal_reached";
    public static final String ADMIN_NEW_CAMPAIGN_REQUEST = "admin_new_campaign_request";
    public static final String ADMIN_NEW_ORG_REGISTERED = "admin_new_org_registered";

    public static final String CAMPAIGN_ID = "campaignId";
    public static final String VOLUNTEER_ID = "volunteerId";
    public static final String DONATION_ID = "donationId";
    public static final String ORGANIZATION_ID = "organizationId";
}
