package com.givinghands.givinghands.service;

/**
 * Sends newsletter emails to active subscribers (invoked from observer listeners).
 */
public interface NewsletterNotificationService {

    void notifyCampaignApprovedActive(Long campaignId);

    void notifyCampaignGoalReached(Long campaignId);

    void notifyVolunteerOpportunityAvailable(Long campaignId);
}
