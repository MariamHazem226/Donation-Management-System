package com.givinghands.givinghands.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(length = 100)
    private String category;

    @Column(name = "goal_amount", nullable = false)
    private Double goalAmount;

    @Column(name = "current_amount", nullable = false)
    private Double currentAmount = 0.0;

    @Column(nullable = false)
    private LocalDate deadline;

    /** PENDING, ACTIVE, APPROVED, REJECTED — must match ck_campaigns_status in database/fix-schema.sql */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "allow_volunteers", nullable = false)
    private boolean allowVolunteers = true;

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    @JsonIgnoreProperties({"campaigns"})
    private User creator;

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getGoalAmount() {
        return goalAmount;
    }

    public void setGoalAmount(Double goalAmount) {
        this.goalAmount = goalAmount;
    }

    public Double getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(Double currentAmount) {
        this.currentAmount = currentAmount;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isAllowVolunteers() {
        return allowVolunteers;
    }

    public void setAllowVolunteers(boolean allowVolunteers) {
        this.allowVolunteers = allowVolunteers;
    }
}
