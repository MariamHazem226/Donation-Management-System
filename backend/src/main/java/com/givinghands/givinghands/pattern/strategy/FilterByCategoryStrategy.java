package com.givinghands.givinghands.pattern.strategy;

import com.givinghands.givinghands.entity.Campaign;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Strategy Pattern — Concrete Strategy #1
 *
 * Filters campaigns by a specific category (e.g. "education", "health").
 * Case-insensitive comparison.
 */
public class FilterByCategoryStrategy implements CampaignFilterStrategy {

    private final String category;

    public FilterByCategoryStrategy(String category) {
        this.category = category;
    }

    @Override
    public List<Campaign> apply(List<Campaign> campaigns) {
        return campaigns.stream()
                .filter(c -> c.getCategory() != null
                        && c.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }
}
