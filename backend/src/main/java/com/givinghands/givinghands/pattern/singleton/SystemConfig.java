package com.givinghands.givinghands.pattern.singleton;

/**
 * Singleton Pattern
 * Holds global system configuration — only one instance exists at runtime.
 */
public class SystemConfig {

    private static SystemConfig instance;

    private String platformName = "GivingHands";
    private String version      = "1.0.0";
    private int    maxDonation  = 100000;

    // Private constructor — prevents direct instantiation
    private SystemConfig() {}

    public static SystemConfig getInstance() {
        if (instance == null) {
            instance = new SystemConfig();
        }
        return instance;
    }

    public String getPlatformName() { return platformName; }
    public String getVersion()      { return version; }
    public int    getMaxDonation()  { return maxDonation; }
}
