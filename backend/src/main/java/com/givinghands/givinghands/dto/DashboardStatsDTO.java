package com.givinghands.givinghands.dto;

public class DashboardStatsDTO {
    private long totalUsers;
    private long totalCampaigns;
    private long totalVolunteers;
    private long activeCampaigns;
    private long pendingCampaigns;

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getTotalCampaigns() { return totalCampaigns; }
    public void setTotalCampaigns(long totalCampaigns) { this.totalCampaigns = totalCampaigns; }

    public long getTotalVolunteers() { return totalVolunteers; }
    public void setTotalVolunteers(long totalVolunteers) { this.totalVolunteers = totalVolunteers; }

    public long getActiveCampaigns() { return activeCampaigns; }
    public void setActiveCampaigns(long activeCampaigns) { this.activeCampaigns = activeCampaigns; }

    public long getPendingCampaigns() { return pendingCampaigns; }
    public void setPendingCampaigns(long pendingCampaigns) { this.pendingCampaigns = pendingCampaigns; }
}