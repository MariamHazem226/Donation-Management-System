package com.givinghands.givinghands.repository;

import com.givinghands.givinghands.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    Optional<Campaign> findByTitleIgnoreCase(String title);

    List<Campaign> findByCreatorId(Long creatorId);
    List<Campaign> findByOrganizationId(Long organizationId);
    long countByStatus(String status);

    List<Campaign> findByStatus(String status);
}


