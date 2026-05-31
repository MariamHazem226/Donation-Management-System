package com.givinghands.givinghands.service.impl;

import com.givinghands.givinghands.dto.DonationDTO;
import com.givinghands.givinghands.entity.Campaign;
import com.givinghands.givinghands.entity.Donation;
import com.givinghands.givinghands.repository.CampaignRepository;
import com.givinghands.givinghands.repository.DonationRepository;
import com.givinghands.givinghands.notification.NotificationEvents;
import com.givinghands.givinghands.notification.NotificationPublisher;
import com.givinghands.givinghands.service.DonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DonationServiceImpl implements DonationService {

    private final DonationRepository donationRepository;
    private final CampaignRepository campaignRepository;
    private final NotificationPublisher notificationPublisher;

    @Override
    public DonationDTO submitDonation(DonationDTO dto) {
        Donation donation = new Donation();
        donation.setUserId(dto.getUserId());
        donation.setCampaignId(dto.getCampaignId());
        donation.setAmount(dto.getAmount());
        donation.setDate(LocalDate.now());
        donation.setStatus("COMPLETED");

        Donation saved = donationRepository.save(donation);

        notificationPublisher.publish(
                NotificationEvents.USER_DONATION_SUBMITTED,
                Map.of(NotificationEvents.DONATION_ID, saved.getId())
        );

        Long campaignId = dto.getCampaignId();
        Double amount = dto.getAmount();

        updateCampaignFunding(campaignId, amount);

        Campaign updatedCampaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        boolean goalReached = updatedCampaign.getGoalAmount() != null
                && updatedCampaign.getCurrentAmount() != null
                && updatedCampaign.getCurrentAmount() >= updatedCampaign.getGoalAmount();
        if (goalReached) {
            Map<String, Object> goalPayload = Map.of(NotificationEvents.CAMPAIGN_ID, campaignId);
            notificationPublisher.publish(NotificationEvents.NEWSLETTER_CAMPAIGN_GOAL_REACHED, goalPayload);
            notificationPublisher.publish(NotificationEvents.ADMIN_CAMPAIGN_GOAL_REACHED, goalPayload);
        }

        return toDTO(saved);


    }

    @Override
    public List<DonationDTO> getDonationHistory(Long userId) {
        return donationRepository.findByUserId(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public void updateCampaignFunding(Long campaignId, Double amount) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        double updated = (campaign.getCurrentAmount() == null ? 0 : campaign.getCurrentAmount()) + amount;
        campaign.setCurrentAmount(updated);
        campaignRepository.save(campaign);
    }

    private DonationDTO toDTO(Donation d) {
        DonationDTO dto = new DonationDTO();
        dto.setId(d.getId());
        dto.setUserId(d.getUserId());
        dto.setCampaignId(d.getCampaignId());
        dto.setAmount(d.getAmount());
        dto.setDate(d.getDate());
        dto.setStatus(d.getStatus());
        // Enrich DTO with campaign title for profile donation history
        Campaign c = campaignRepository.findById(d.getCampaignId()).orElse(null);
        if (c != null) dto.setCampaignTitle(c.getTitle());
        return dto;
    }
}
