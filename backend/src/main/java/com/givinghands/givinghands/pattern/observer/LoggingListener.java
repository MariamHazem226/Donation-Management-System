package com.givinghands.givinghands.pattern.observer;

public class LoggingListener implements CampaignEventListener {

    private String logFile;

    public LoggingListener(String logFile) {
        this.logFile = logFile;
    }

    @Override
    public void update(String eventType, Long campaignId, String details) {
        System.out.println("Save to log [" + logFile + "]: Event [" + eventType + "] on Campaign ID " + campaignId + " — " + details);
    }
}
