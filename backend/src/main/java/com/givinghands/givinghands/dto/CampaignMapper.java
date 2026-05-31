package com.givinghands.givinghands.dto;

import com.givinghands.givinghands.entity.Campaign;

public final class CampaignMapper {

    private CampaignMapper() {
    }

    public static CampaignDTO toDto(Campaign campaign) {
        if (campaign == null) {
            return null;
        }
        CampaignDTO dto = new CampaignDTO();
        dto.setId(campaign.getId());
        dto.setTitle(campaign.getTitle());
        dto.setDescription(campaign.getDescription());
        dto.setCategory(campaign.getCategory());
        dto.setGoalAmount(campaign.getGoalAmount());
        dto.setCurrentAmount(campaign.getCurrentAmount());
        dto.setDeadline(campaign.getDeadline());
        dto.setStatus(campaign.getStatus());
        dto.setOrganizationId(campaign.getOrganizationId());
        // The organization display name is stored in creator.user.name
        // (creator is the organization user that created the campaign)
        dto.setOrganizationName(
                campaign.getCreator() != null ? campaign.getCreator().getName() : null);
        dto.setImage(campaign.getImagePath());

        dto.setAllowVolunteers(campaign.isAllowVolunteers());
        return dto;
    }
}
