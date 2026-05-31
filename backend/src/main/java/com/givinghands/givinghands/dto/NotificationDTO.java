package com.givinghands.givinghands.dto;

public class NotificationDTO {
    private String eventType;
    private Long campaignId;
    private String details;

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}