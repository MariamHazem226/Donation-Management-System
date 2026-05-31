package com.givinghands.givinghands.service;

/**
 * Sends email notifications to end users (invoked from observer listeners).
 */
public interface UserNotificationService {

    void notifyDonationSubmitted(Long donationId);

    void notifyVolunteerApproved(Long volunteerId);

    void notifyVolunteerRejected(Long volunteerId);
}
