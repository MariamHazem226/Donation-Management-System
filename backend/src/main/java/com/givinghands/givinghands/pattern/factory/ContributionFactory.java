package com.givinghands.givinghands.pattern.factory;

import com.givinghands.givinghands.entity.Donation;
import com.givinghands.givinghands.entity.Volunteer;

/**
 * Factory Pattern
 * Creates entity objects (Donation / Volunteer) without exposing new() calls directly.
 */
public class ContributionFactory {

    public static Object create(String type) {
        switch (type.toUpperCase()) {
            case "DONATION":
                Donation donation = new Donation();
                donation.setStatus("PENDING");
                return donation;
            case "VOLUNTEER":
                Volunteer volunteer = new Volunteer();
                volunteer.setStatus("PENDING");
                return volunteer;
            default:
                throw new IllegalArgumentException("Unknown contribution type: " + type);
        }
    }
}
