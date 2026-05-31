package com.givinghands.givinghands.service;

import com.givinghands.givinghands.dto.CampaignDTO;

import java.util.List;

public interface OrganizationService {

    List<CampaignDTO> getCampaignsByOrganization(Long organizationId);

    List<CampaignDTO> getVolunteersForOrganizationCampaigns(Long organizationId);

    CampaignDTO trackCampaignProgress(Long campaignId);
}
