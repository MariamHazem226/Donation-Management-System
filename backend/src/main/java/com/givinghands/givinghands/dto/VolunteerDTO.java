package com.givinghands.givinghands.dto;

import java.time.LocalDate;

public class VolunteerDTO {
    private Long id;
    private Long userId;
    private Long campaignId;
    private String whyJoin;
    private String skills;
    private String availability;
    private String experience;
    private String phone;
    private String status;
    private String volunteerName;
    private String email;
    private String campaignTitle;
    private LocalDate appliedDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }

    public String getWhyJoin() { return whyJoin; }
    public void setWhyJoin(String whyJoin) { this.whyJoin = whyJoin; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVolunteerName() { return volunteerName; }
    public void setVolunteerName(String volunteerName) { this.volunteerName = volunteerName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCampaignTitle() { return campaignTitle; }
    public void setCampaignTitle(String campaignTitle) { this.campaignTitle = campaignTitle; }

    public LocalDate getAppliedDate() { return appliedDate; }
    public void setAppliedDate(LocalDate appliedDate) { this.appliedDate = appliedDate; }
}