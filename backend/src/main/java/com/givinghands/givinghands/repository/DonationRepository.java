package com.givinghands.givinghands.repository;

import com.givinghands.givinghands.entity.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long> {
    List<Donation> findByUserId(Long userId);
    boolean existsByUserIdAndCampaignIdAndAmount(Long userId, Long campaignId, Double amount);
}