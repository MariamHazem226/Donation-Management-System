package com.givinghands.givinghands.pattern.observer;

public class ObserverDemo {

    public static void main(String[] args) {

        CampaignPublisher publisher = new CampaignPublisher();

        CampaignEventListener logListener = new LoggingListener("givinghands.log");

        publisher.getEvents().subscribe("campaign_created",  logListener);
        publisher.getEvents().subscribe("campaign_approved", logListener);
        publisher.getEvents().subscribe("volunteer_applied", logListener);
        publisher.getEvents().subscribe("donation_received", logListener);

        // Simulate events
        publisher.onCampaignCreated(1L, "Help Gaza Children");
        publisher.onCampaignApproved(1L, "Help Gaza Children");
        publisher.onVolunteerApplied(1L, "Sara Ahmed");
        publisher.onDonationReceived(1L, 500.0);

        publisher.getEvents().unsubscribe("donation_received", logListener);
        publisher.onDonationReceived(1L, 200.0);
    }
}