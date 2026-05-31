package com.givinghands.givinghands.service;

import com.givinghands.givinghands.dto.CampaignDTO;

import java.util.List;

public interface CampaignService {

    CampaignDTO createCampaign(CampaignDTO campaignDTO);

    CampaignDTO updateCampaign(Long id, CampaignDTO campaignDTO);

    void deleteCampaign(Long id);

    List<CampaignDTO> getAllCampaigns();

    CampaignDTO getCampaignById(Long id);

    List<CampaignDTO> filterCampaigns(String category, String status);

    List<CampaignDTO> getCampaignsByStatus(String status);
}
