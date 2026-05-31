package com.givinghands.givinghands.pattern.strategy;

import com.givinghands.givinghands.entity.Campaign;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy Pattern — Context
 *
 * Holds a reference to a CampaignFilterStrategy and delegates the filtering
 * operation to whichever strategy is currently set.
 *
 * Usage example inside CampaignServiceImpl:
 *
 *   CampaignContext context = new CampaignContext();
 *
 *   // Filter by category
 *   context.setStrategy(new FilterByCategoryStrategy("health"));
 *   List<Campaign> healthCampaigns = context.executeStrategy(allCampaigns);
 *
 *   // Sort by deadline
 *   context.setStrategy(new SortByDeadlineStrategy());
 *   List<Campaign> sorted = context.executeStrategy(allCampaigns);
 */
@Component
public class CampaignContext {

    private CampaignFilterStrategy strategy;

    public CampaignContext() {}

    public CampaignContext(CampaignFilterStrategy strategy) {
        this.strategy = strategy;
    }

    /** Set (or swap) the strategy at runtime. */
    public void setStrategy(CampaignFilterStrategy strategy) {
        this.strategy = strategy;
    }

    /** Delegate execution to the current strategy. */
    public List<Campaign> executeStrategy(List<Campaign> campaigns) {
        if (strategy == null) {
            throw new IllegalStateException("No filter strategy has been set.");
        }
        return strategy.apply(campaigns);
    }
}
