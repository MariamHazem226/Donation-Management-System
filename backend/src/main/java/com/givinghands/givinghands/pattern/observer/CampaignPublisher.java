package com.givinghands.givinghands.pattern.observer;

public class CampaignPublisher {

    public CampaignEventManager events;

    public CampaignPublisher() {
        this.events = new CampaignEventManager(
                "campaign_created",
                "campaign_approved",
                "campaign_rejected",
                "volunteer_applied",
                "donation_received"
        );
    }

    public CampaignEventManager getEvents() {
        return this.events;
    }

    public void onCampaignCreated(Long campaignId, String title) {
        events.notify("campaign_created", campaignId, "Campaign created: " + title);
    }

    public void onCampaignApproved(Long campaignId, String title) {
        events.notify("campaign_approved", campaignId, "Campaign approved: " + title);
    }

    public void onCampaignRejected(Long campaignId, String title) {
        events.notify("campaign_rejected", campaignId, "Campaign rejected: " + title);
    }

    public void onVolunteerApplied(Long campaignId, String volunteerName) {
        events.notify("volunteer_applied", campaignId, "New volunteer applied: " + volunteerName);
    }

    public void onDonationReceived(Long campaignId, Double amount) {
        events.notify("donation_received", campaignId, "New donation received: $" + amount);
    }
}
