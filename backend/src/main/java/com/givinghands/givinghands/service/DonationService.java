package com.givinghands.givinghands.service;

import com.givinghands.givinghands.dto.DonationDTO;
import java.util.List;

public interface DonationService {
    DonationDTO submitDonation(DonationDTO dto);
    List<DonationDTO> getDonationHistory(Long userId);
    void updateCampaignFunding(Long campaignId, Double amount);
}