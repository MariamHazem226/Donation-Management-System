package com.givinghands.givinghands.service.impl;

import com.givinghands.givinghands.dto.CampaignDTO;
import com.givinghands.givinghands.entity.Campaign;
import com.givinghands.givinghands.repository.CampaignRepository;
import com.givinghands.givinghands.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    @Autowired
    private CampaignRepository campaignRepository;

    // ─── GET CAMPAIGNS BY ORGANIZATION ────────────────────────────────────────

    @Override
    public List<CampaignDTO> getCampaignsByOrganization(Long organizationId) {
        return campaignRepository.findByOrganizationId(organizationId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ─── GET VOLUNTEERS FOR ORGANIZATION CAMPAIGNS ────────────────────────────

    @Override
    public List<CampaignDTO> getVolunteersForOrganizationCampaigns(Long organizationId) {
        // Returns campaigns that have at least one volunteer (status = ACTIVE)
        return campaignRepository.findByOrganizationId(organizationId)
                .stream()
                .filter(c -> "APPROVED".equalsIgnoreCase(c.getStatus()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ─── TRACK CAMPAIGN PROGRESS ──────────────────────────────────────────────

    @Override
    public CampaignDTO trackCampaignProgress(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + campaignId));
        return mapToDTO(campaign);
    }

    // ─── MAPPER ───────────────────────────────────────────────────────────────

    private CampaignDTO mapToDTO(Campaign c) {
        CampaignDTO dto = new CampaignDTO();
        dto.setId(c.getId());
        dto.setTitle(c.getTitle());
        dto.setDescription(c.getDescription());
        dto.setCategory(c.getCategory());
        dto.setGoalAmount(c.getGoalAmount());
        dto.setCurrentAmount(c.getCurrentAmount());
        dto.setDeadline(c.getDeadline());
        dto.setStatus(c.getStatus());
        dto.setOrganizationId(c.getOrganizationId());
        dto.setImage(c.getImagePath());
        return dto;
    }

}
