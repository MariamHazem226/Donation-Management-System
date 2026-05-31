package com.givinghands.givinghands.dto;

import java.time.LocalDate;

public class CampaignDTO {

    private Long id;
    private String title;
    private String description;
    private String category;
    private Double goalAmount;
    private Double currentAmount;
    private LocalDate deadline;
    private String status;
    private Long organizationId;

    // Organizer/organization display name (used in campaign details UI)
    private String organizationName;


    private String image;

    private Boolean allowVolunteers;

    public CampaignDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getGoalAmount() { return goalAmount; }
    public void setGoalAmount(Double goalAmount) { this.goalAmount = goalAmount; }

    public Double getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(Double currentAmount) { this.currentAmount = currentAmount; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }


    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public Boolean getAllowVolunteers() { return allowVolunteers; }
    public void setAllowVolunteers(Boolean allowVolunteers) { this.allowVolunteers = allowVolunteers; }
}

