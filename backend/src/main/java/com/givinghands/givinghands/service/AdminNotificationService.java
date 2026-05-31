package com.givinghands.givinghands.service;

/**
 * Sends email notifications to platform administrators (invoked from observer listeners).
 */
public interface AdminNotificationService {

    void notifyCampaignGoalReached(Long campaignId);

    void notifyNewCampaignRequest(Long campaignId);

    void notifyNewOrganizationRegistered(Long organizationId);
}
