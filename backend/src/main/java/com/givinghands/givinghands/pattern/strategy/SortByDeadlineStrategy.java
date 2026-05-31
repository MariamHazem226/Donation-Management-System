package com.givinghands.givinghands.pattern.strategy;

import com.givinghands.givinghands.entity.Campaign;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Strategy Pattern — Concrete Strategy #3
 *
 * Sorts campaigns by deadline (closest deadline first).
 * Campaigns with null deadlines are pushed to the end.
 */
public class SortByDeadlineStrategy implements CampaignFilterStrategy {

    @Override
    public List<Campaign> apply(List<Campaign> campaigns) {
        return campaigns.stream()
                .sorted(Comparator.comparing(
                        Campaign::getDeadline,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }
}
