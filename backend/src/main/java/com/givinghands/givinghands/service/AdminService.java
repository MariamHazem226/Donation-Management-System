package com.givinghands.givinghands.service;

import java.util.List;
import java.util.Map;

import com.givinghands.givinghands.entity.Campaign;
import com.givinghands.givinghands.entity.User;
import com.givinghands.givinghands.entity.Volunteer;

public interface AdminService {

    Map<String, Object> getDashboardStats();

    List<User> getAllUsers();

    List<Campaign> getAllCampaigns();

    List<Volunteer> getAllVolunteers();

    java.util.List<com.givinghands.givinghands.dto.AdminDonationDTO> getAllDonations();

    java.util.List<com.givinghands.givinghands.dto.AdminVolunteerDTO> getAllVolunteersForDashboard();


    void deleteUser(Long id);

    void deleteVolunteer(Long id);


    void deleteCampaign(Long id);

    Campaign approveCampaign(Long id);

    Campaign rejectCampaign(Long id);
}
