package com.givinghands.givinghands.pattern.observer;

public interface CampaignEventListener {
    void update(String eventType, Long campaignId, String details);
}
