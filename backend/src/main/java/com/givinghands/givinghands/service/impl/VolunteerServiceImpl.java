package com.givinghands.givinghands.service.impl;

import com.givinghands.givinghands.dto.VolunteerDTO;
import com.givinghands.givinghands.entity.Volunteer;
import com.givinghands.givinghands.entity.User;
import com.givinghands.givinghands.entity.Campaign;
import com.givinghands.givinghands.repository.VolunteerRepository;
import com.givinghands.givinghands.repository.UserRepository;
import com.givinghands.givinghands.repository.CampaignRepository;
import com.givinghands.givinghands.notification.NotificationEvents;
import com.givinghands.givinghands.notification.NotificationPublisher;
import com.givinghands.givinghands.service.VolunteerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VolunteerServiceImpl implements VolunteerService {

    private final VolunteerRepository volunteerRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final NotificationPublisher notificationPublisher;

    @Override
    public VolunteerDTO applyVolunteer(VolunteerDTO dto) {
        if (dto.getUserId() == null) {
            throw new RuntimeException("User id is required");
        }
        if (dto.getCampaignId() == null) {
            throw new RuntimeException("Campaign id is required");
        }
        if (dto.getWhyJoin() == null || dto.getWhyJoin().isBlank()) {
            throw new RuntimeException("Please provide a reason for volunteering");
        }

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Campaign campaign = campaignRepository.findById(dto.getCampaignId())
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        // Only normal users can apply as volunteers.
        // Organizations should not be able to submit volunteer applications.
        String role = user.getRole() != null ? user.getRole().trim().toUpperCase() : "USER";
        if ("ORGANIZATION".equals(role)) {
            throw new RuntimeException("Volunteer applications are not allowed for organization accounts");
        }

        if (!campaign.isAllowVolunteers()) {
            throw new RuntimeException("This campaign is not accepting volunteer applications");
        }

        String status = campaign.getStatus() != null ? campaign.getStatus().toUpperCase() : "";
        if (!"APPROVED".equals(status) && !"PENDING".equals(status)) {
            throw new RuntimeException("Volunteer applications are only open for approved campaigns");
        }

        Volunteer volunteer = new Volunteer();
        volunteer.setUser(user);
        volunteer.setCampaign(campaign);
        volunteer.setStatus("PENDING");
        volunteer.setAppliedDate(LocalDate.now());
        volunteer.setWhyJoin(dto.getWhyJoin());
        volunteer.setSkills(dto.getSkills());
        volunteer.setAvailability(dto.getAvailability());
        volunteer.setExperience(dto.getExperience());
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            volunteer.setPhone(dto.getPhone().trim());
        }

        Volunteer saved = volunteerRepository.save(volunteer);

        notificationPublisher.publish(
                NotificationEvents.ORG_VOLUNTEER_APPLIED,
                Map.of(NotificationEvents.VOLUNTEER_ID, saved.getId())
        );

        return toDTO(saved);
    }


    @Override
    public VolunteerDTO approveVolunteer(Long id) {

        Volunteer volunteer = volunteerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Volunteer not found"));

        volunteer.setStatus("ACCEPTED");
        Volunteer saved = volunteerRepository.save(volunteer);

        notificationPublisher.publish(
                NotificationEvents.USER_VOLUNTEER_APPROVED,
                Map.of(NotificationEvents.VOLUNTEER_ID, saved.getId())
        );

        return toDTO(saved);
    }

    @Override
    public VolunteerDTO rejectVolunteer(Long id) {
        Volunteer volunteer = volunteerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Volunteer not found"));

        volunteer.setStatus("REJECTED");
        Volunteer saved = volunteerRepository.save(volunteer);

        notificationPublisher.publish(
                NotificationEvents.USER_VOLUNTEER_REJECTED,
                Map.of(NotificationEvents.VOLUNTEER_ID, saved.getId())
        );

        return toDTO(saved);
    }


    @Override
    public void deleteVolunteer(Long id) {
        if (!volunteerRepository.existsById(id)) {
            throw new RuntimeException("Volunteer not found with id: " + id);
        }
        volunteerRepository.deleteById(id);

    }

    @Override
    public List<VolunteerDTO> getVolunteersByCampaign(Long campaignId) {
        return volunteerRepository.findByCampaign_Id(campaignId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<VolunteerDTO> getVolunteersByUser(Long userId) {
        return volunteerRepository.findByUser_Id(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public VolunteerDTO getVolunteerById(Long id) {
        Volunteer volunteer = volunteerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Volunteer not found"));
        return toDTO(volunteer);
    }

    private VolunteerDTO toDTO(Volunteer v) {


        VolunteerDTO dto = new VolunteerDTO();
        dto.setId(v.getId());
        dto.setUserId(v.getUser().getId());
        dto.setCampaignId(v.getCampaign().getId());
        dto.setWhyJoin(v.getWhyJoin());
        dto.setSkills(v.getSkills());
        dto.setAvailability(v.getAvailability());
        dto.setExperience(v.getExperience());
        dto.setPhone(v.getPhone());
        dto.setStatus(v.getStatus());
        dto.setAppliedDate(v.getAppliedDate());
        if (v.getUser() != null) {
            dto.setVolunteerName(v.getUser().getName());
            dto.setEmail(v.getUser().getEmail());
        }
        if (v.getCampaign() != null) {
            dto.setCampaignTitle(v.getCampaign().getTitle());
        }
        return dto;
    }
}