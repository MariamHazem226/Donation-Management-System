package com.givinghands.givinghands.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "volunteers")
public class Volunteer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDate appliedDate;

    @Column
    private String whyJoin;

    @Column
    private String skills;

    @Column
    private String availability;

    @Column
    private String experience;

    /** Contact phone provided on the application (for organization outreach). */
    @Column(length = 50)
    private String phone;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Campaign getCampaign() { return campaign; }
    public void setCampaign(Campaign campaign) { this.campaign = campaign; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getAppliedDate() { return appliedDate; }
    public void setAppliedDate(LocalDate appliedDate) { this.appliedDate = appliedDate; }

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
}