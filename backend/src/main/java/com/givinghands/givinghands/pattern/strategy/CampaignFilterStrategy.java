package com.givinghands.givinghands.pattern.strategy;

import com.givinghands.givinghands.entity.Campaign;

import java.util.List;

/**
 * Strategy Pattern — Strategy Interface
 *
 * Defines a common contract for different campaign filtering/sorting strategies.
 * Each concrete strategy implements its own filtering logic independently,
 * so we can swap strategies at runtime without changing the context (CampaignService).
 *
 * Example strategies:
 *   - FilterByCategory   → filters campaigns by a given category
 *   - FilterByStatus     → filters campaigns by their current status
 *   - SortByDeadline     → sorts campaigns by closest deadline
 *   - SortByProgress     → sorts campaigns by funding progress
 */
public interface CampaignFilterStrategy {

    /**
     * Apply this strategy to a list of campaigns.
     *
     * @param campaigns the full list of campaigns to process
     * @return the filtered or sorted result
     */
    List<Campaign> apply(List<Campaign> campaigns);
}
