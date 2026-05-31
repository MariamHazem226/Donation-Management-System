package com.givinghands.givinghands.service.impl;

import com.givinghands.givinghands.dto.CampaignDTO;
import com.givinghands.givinghands.entity.Campaign;
import com.givinghands.givinghands.entity.User;
import com.givinghands.givinghands.repository.CampaignRepository;
import com.givinghands.givinghands.repository.UserRepository;
import com.givinghands.givinghands.notification.NotificationEvents;
import com.givinghands.givinghands.notification.NotificationPublisher;
import com.givinghands.givinghands.service.CampaignService;
import com.givinghands.givinghands.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CampaignServiceImpl implements CampaignService {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationPublisher notificationPublisher;

    // ─── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public CampaignDTO createCampaign(CampaignDTO campaignDTO) {

        if (!ValidationUtil.notEmpty(campaignDTO.getTitle()))
            throw new RuntimeException("Campaign title is required");

        if (campaignDTO.getGoalAmount() == null || campaignDTO.getGoalAmount() <= 0)
            throw new RuntimeException("Goal amount must be positive");

        if (campaignDTO.getDeadline() == null)
            throw new RuntimeException("Campaign deadline is required");

        if (campaignDTO.getOrganizationId() == null)
            throw new RuntimeException("Organization id is required");

        User creator = userRepository.findById(campaignDTO.getOrganizationId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        String creatorRole = creator.getRole() != null ? creator.getRole().trim().toUpperCase() : "";
        if (!"ORGANIZATION".equals(creatorRole) && !"ADMIN".equals(creatorRole))
            throw new RuntimeException("Only organization accounts can create campaigns");

        String title = ValidationUtil.sanitize(campaignDTO.getTitle());
        String description = ValidationUtil.sanitize(campaignDTO.getDescription());
        if (!ValidationUtil.notEmpty(description))
            description = title;

        Campaign campaign = new Campaign();
        campaign.setTitle(title);
        campaign.setDescription(description);
        campaign.setCategory(ValidationUtil.sanitize(campaignDTO.getCategory()));
        campaign.setGoalAmount(campaignDTO.getGoalAmount());
        campaign.setCurrentAmount(0.0);
        campaign.setDeadline(campaignDTO.getDeadline());
        campaign.setStatus("PENDING");
        campaign.setOrganizationId(campaignDTO.getOrganizationId());
        campaign.setAllowVolunteers(campaignDTO.getAllowVolunteers() == null || campaignDTO.getAllowVolunteers());
        campaign.setCreator(creator);

        Campaign saved = campaignRepository.save(campaign);

        notificationPublisher.publish(
                NotificationEvents.ADMIN_NEW_CAMPAIGN_REQUEST,
                Map.of(NotificationEvents.CAMPAIGN_ID, saved.getId())
        );

        return mapToDTO(saved);
    }

    // ─── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public CampaignDTO updateCampaign(Long id, CampaignDTO campaignDTO) {

        Campaign existing = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));

        if (campaignDTO.getTitle() != null)
            existing.setTitle(ValidationUtil.sanitize(campaignDTO.getTitle()));

        if (campaignDTO.getDescription() != null)
            existing.setDescription(ValidationUtil.sanitize(campaignDTO.getDescription()));

        if (campaignDTO.getCategory() != null)
            existing.setCategory(ValidationUtil.sanitize(campaignDTO.getCategory()));

        if (campaignDTO.getGoalAmount() != null)
            existing.setGoalAmount(campaignDTO.getGoalAmount());

        if (campaignDTO.getDeadline() != null)
            existing.setDeadline(campaignDTO.getDeadline());

        if (campaignDTO.getStatus() != null)
            existing.setStatus(campaignDTO.getStatus());

        if (campaignDTO.getImage() != null)
            existing.setImagePath(campaignDTO.getImage());

        if (campaignDTO.getAllowVolunteers() != null)
            existing.setAllowVolunteers(campaignDTO.getAllowVolunteers());

        Campaign updated = campaignRepository.save(existing);
        return mapToDTO(updated);
    }

    // ─── DELETE ────────────────────────────────────────────────────────────────

    @Override
    public void deleteCampaign(Long id) {
        if (!campaignRepository.existsById(id)) {
            throw new RuntimeException("Campaign not found with id: " + id);
        }
        campaignRepository.deleteById(id);
    }

    // ─── GET ALL ───────────────────────────────────────────────────────────────

    @Override
    public List<CampaignDTO> getAllCampaigns() {
        return campaignRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ─── GET BY ID ─────────────────────────────────────────────────────────────

    @Override
    public CampaignDTO getCampaignById(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));
        return mapToDTO(campaign);
    }

    // ─── FILTER ────────────────────────────────────────────────────────────────

    @Override
    public List<CampaignDTO> filterCampaigns(String category, String status) {
        return campaignRepository.findAll()
                .stream()
                .filter(c -> category == null ||
                        (c.getCategory() != null &&
                                c.getCategory().equalsIgnoreCase(category.trim())))
                .filter(c -> status == null ||
                        c.getStatus().equalsIgnoreCase(status.trim()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ─── GET BY STATUS ─────────────────────────────────────────────────────────

    @Override
    public List<CampaignDTO> getCampaignsByStatus(String status) {
        return campaignRepository.findByStatus(status)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ─── MAPPER ────────────────────────────────────────────────────────────────

    private CampaignDTO mapToDTO(Campaign c) {
        CampaignDTO dto = new CampaignDTO();

        dto.setId(c.getId());
        dto.setTitle(ValidationUtil.sanitize(c.getTitle()));
        dto.setDescription(ValidationUtil.sanitize(c.getDescription()));
        dto.setCategory(ValidationUtil.sanitize(c.getCategory()));

        dto.setGoalAmount(c.getGoalAmount());
        dto.setCurrentAmount(c.getCurrentAmount());
        dto.setDeadline(c.getDeadline());
        dto.setStatus(c.getStatus());
        dto.setOrganizationId(c.getOrganizationId());
        dto.setOrganizationName(c.getCreator() != null ? c.getCreator().getName() : null);
        dto.setImage(c.getImagePath());
        dto.setAllowVolunteers(c.isAllowVolunteers());


        return dto;
    }
}

