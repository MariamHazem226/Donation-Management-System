package com.givinghands.givinghands.pattern.strategy;

import com.givinghands.givinghands.entity.Campaign;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Strategy Pattern — Concrete Strategy #2
 *
 * Filters campaigns by their current status (e.g. "APPROVED", "PENDING", "REJECTED").
 * Case-insensitive comparison.
 */
public class FilterByStatusStrategy implements CampaignFilterStrategy {

    private final String status;

    public FilterByStatusStrategy(String status) {
        this.status = status;
    }

    @Override
    public List<Campaign> apply(List<Campaign> campaigns) {
        return campaigns.stream()
                .filter(c -> c.getStatus() != null
                        && c.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());
    }
}
