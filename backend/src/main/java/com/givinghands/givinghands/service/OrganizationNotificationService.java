package com.givinghands.givinghands.service;

/**
 * Sends email notifications to campaign organizations (invoked from observer listeners).
 */
public interface OrganizationNotificationService {

    void notifyOrganizationRegistered(Long organizationId);

    void notifyOrganizationWelcomeApproved(Long organizationId);

    void notifyVolunteerApplied(Long volunteerId);

    void notifyCampaignApproved(Long campaignId);

    void notifyCampaignRejected(Long campaignId);
}
