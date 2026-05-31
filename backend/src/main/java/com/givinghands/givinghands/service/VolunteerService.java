package com.givinghands.givinghands.service;

import com.givinghands.givinghands.dto.VolunteerDTO;
import java.util.List;

public interface VolunteerService {
    VolunteerDTO applyVolunteer(VolunteerDTO volunteerDTO);
    VolunteerDTO approveVolunteer(Long id);
    VolunteerDTO rejectVolunteer(Long id);
    void deleteVolunteer(Long id);

    List<VolunteerDTO> getVolunteersByCampaign(Long campaignId);
    List<VolunteerDTO> getVolunteersByUser(Long userId);

    VolunteerDTO getVolunteerById(Long id);
}