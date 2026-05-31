package com.givinghands.givinghands.service.impl;

import com.givinghands.givinghands.entity.Campaign;
import com.givinghands.givinghands.entity.User;
import com.givinghands.givinghands.entity.Volunteer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.givinghands.givinghands.repository.CampaignRepository;
import com.givinghands.givinghands.repository.DonationRepository;
import com.givinghands.givinghands.repository.UserRepository;
import com.givinghands.givinghands.repository.VolunteerRepository;
import com.givinghands.givinghands.notification.NotificationEvents;
import com.givinghands.givinghands.notification.NotificationPublisher;
import com.givinghands.givinghands.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private VolunteerRepository volunteerRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private NotificationPublisher notificationPublisher;

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);


    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalCampaigns", campaignRepository.count());
        stats.put("totalVolunteers", volunteerRepository.count());
        stats.put("approvedCampaigns", campaignRepository.countByStatus("APPROVED"));
        stats.put("pendingCampaigns", campaignRepository.countByStatus("PENDING"));

        double totalRaised = donationRepository.findAll()
                .stream()
                .mapToDouble(d -> d.getAmount() != null ? d.getAmount() : 0)
                .sum();

        stats.put("totalRaised", totalRaised);

        return stats;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    @Override
    public List<Volunteer> getAllVolunteers() {
        return volunteerRepository.findAll();
    }

    @Override
    public java.util.List<com.givinghands.givinghands.dto.AdminDonationDTO> getAllDonations() {
        return donationRepository.findAll()
                .stream()
                .map(this::toAdminDonationDTO)
                .sorted((a, b) -> {
                    if (a.getDate() == null && b.getDate() == null) return 0;
                    if (a.getDate() == null) return 1;
                    if (b.getDate() == null) return -1;
                    return b.getDate().compareTo(a.getDate());
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public java.util.List<com.givinghands.givinghands.dto.AdminVolunteerDTO> getAllVolunteersForDashboard() {
        return volunteerRepository.findAll()
                .stream()
                .map(this::toAdminVolunteerDTO)
                .sorted((a, b) -> {
                    if (a.getJoinedDate() == null && b.getJoinedDate() == null) return 0;
                    if (a.getJoinedDate() == null) return 1;
                    if (b.getJoinedDate() == null) return -1;
                    return b.getJoinedDate().compareTo(a.getJoinedDate());
                })
                .collect(java.util.stream.Collectors.toList());
    }


    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public void deleteCampaign(Long id) {
        if (!campaignRepository.existsById(id)) {
            throw new RuntimeException("Campaign not found with id: " + id);
        }
        campaignRepository.deleteById(id);
    }

    @Override
    public void deleteVolunteer(Long id) {
        if (!volunteerRepository.existsById(id)) {
            throw new RuntimeException("Volunteer not found with id: " + id);
        }

        volunteerRepository.deleteById(id);

    }





    @Override
    public Campaign approveCampaign(Long id) {

        log.info("Admin approving campaign id={}", id);

        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));

        campaign.setStatus("APPROVED");

        Campaign saved = campaignRepository.save(campaign);

        log.info("Admin approved campaign id={}, newStatus={}", id, saved.getStatus());

        publishApprovalNotifications(saved);

        return saved;
    }


    private com.givinghands.givinghands.dto.AdminDonationDTO toAdminDonationDTO(com.givinghands.givinghands.entity.Donation donation) {
        com.givinghands.givinghands.dto.AdminDonationDTO dto = new com.givinghands.givinghands.dto.AdminDonationDTO();
        dto.setId(donation.getId());
        dto.setUserId(donation.getUserId());
        dto.setCampaignId(donation.getCampaignId());
        dto.setAmount(donation.getAmount());
        dto.setDate(donation.getDate());
        dto.setStatus(donation.getStatus());

        // Best-effort enrichment (avoid failures if relationships aren't present)
        try {
            dto.setDonorName(userRepository.findById(donation.getUserId()).map(com.givinghands.givinghands.entity.User::getName).orElse(null));
        } catch (Exception ignored) {
            dto.setDonorName(null);
        }

        try {
            dto.setCampaignTitle(campaignRepository.findById(donation.getCampaignId()).map(com.givinghands.givinghands.entity.Campaign::getTitle).orElse(null));
        } catch (Exception ignored) {
            dto.setCampaignTitle(null);
        }

        return dto;
    }

    private com.givinghands.givinghands.dto.AdminVolunteerDTO toAdminVolunteerDTO(Volunteer volunteer) {
        com.givinghands.givinghands.dto.AdminVolunteerDTO dto = new com.givinghands.givinghands.dto.AdminVolunteerDTO();
        dto.setId(volunteer.getId());
        dto.setCampaignId(volunteer.getCampaign() != null ? volunteer.getCampaign().getId() : null);
        dto.setCampaignTitle(volunteer.getCampaign() != null ? volunteer.getCampaign().getTitle() : null);
        dto.setJoinedDate(volunteer.getAppliedDate());
        dto.setStatus(normalizeVolunteerStatus(volunteer.getStatus()));

        dto.setEmail(volunteer.getUser() != null ? volunteer.getUser().getEmail() : null);
        dto.setPhone(volunteer.getPhone());
        String name = volunteer.getUser() != null ? volunteer.getUser().getName() : null;
        dto.setVolunteerName(name);

        // Admin "hours" should represent the schedule/availability the volunteer chose
        dto.setHours(volunteer.getAvailability() != null ? volunteer.getAvailability() : "");
        return dto;
    }

    private String normalizeVolunteerStatus(String status) {
        if (status == null) return null;
        String s = status.toUpperCase();
        if ("ACCEPTED".equals(s)) return "APPROVED"; // frontend label
        return s;
    }

    @Override
    public Campaign rejectCampaign(Long id) {


        log.info("Admin rejecting campaign id={}", id);

        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));

        campaign.setStatus("REJECTED");

        Campaign saved = campaignRepository.save(campaign);

        log.info("Admin rejected campaign id={}, newStatus={}", id, saved.getStatus());

        try {
            notificationPublisher.publish(
                    NotificationEvents.ORG_CAMPAIGN_REJECTED,
                    Map.of(NotificationEvents.CAMPAIGN_ID, saved.getId())
            );
        } catch (Exception e) {
            log.error("Campaign rejected in DB but notification dispatch failed for id={}: {}",
                    id, e.getMessage(), e);
        }

        return saved;
    }

    private void publishApprovalNotifications(Campaign saved) {
        try {
            Map<String, Object> payload = Map.of(NotificationEvents.CAMPAIGN_ID, saved.getId());
            notificationPublisher.publish(NotificationEvents.ORG_CAMPAIGN_APPROVED, payload);
            notificationPublisher.publish(NotificationEvents.NEWSLETTER_CAMPAIGN_APPROVED_ACTIVE, payload);
            if (saved.isAllowVolunteers()) {
                notificationPublisher.publish(NotificationEvents.NEWSLETTER_VOLUNTEER_OPPORTUNITY, payload);
            }
        } catch (Exception e) {
            log.error("Campaign approved in DB but notification dispatch failed for id={}: {}",
                    saved.getId(), e.getMessage(), e);
        }
    }
}