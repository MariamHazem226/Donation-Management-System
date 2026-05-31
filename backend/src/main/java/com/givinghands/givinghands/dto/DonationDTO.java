package com.givinghands.givinghands.dto;

import java.time.LocalDate;

public class DonationDTO {
    private Long id;
    private Long userId;
    private Long campaignId;
    private Double amount;
    private LocalDate date;
    private String status;
    private String campaignTitle;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCampaignTitle() { return campaignTitle; }
    public void setCampaignTitle(String campaignTitle) { this.campaignTitle = campaignTitle; }
}