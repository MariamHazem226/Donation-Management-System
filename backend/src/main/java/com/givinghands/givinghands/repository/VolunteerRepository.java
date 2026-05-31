package com.givinghands.givinghands.repository;

import com.givinghands.givinghands.entity.Volunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VolunteerRepository extends JpaRepository<Volunteer, Long> {
    List<Volunteer> findByCampaign_Id(Long campaignId);
    List<Volunteer> findByUser_Id(Long userId);
    boolean existsByUser_IdAndCampaign_Id(Long userId, Long campaignId);
}