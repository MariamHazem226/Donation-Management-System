package com.givinghands.givinghands.pattern.observer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CampaignEventManager {

    Map<String, List<CampaignEventListener>> listeners = new HashMap<>();

    public CampaignEventManager(String... operations) {
        for (String operation : operations) {
            this.listeners.put(operation, new ArrayList<>());
        }
    }

    public void subscribe(String eventType, CampaignEventListener listener) {
        List<CampaignEventListener> users = listeners.get(eventType);
        users.add(listener);
    }

    public void unsubscribe(String eventType, CampaignEventListener listener) {
        List<CampaignEventListener> users = listeners.get(eventType);
        users.remove(listener);
    }

    public void notify(String eventType, Long campaignId, String details) {
        List<CampaignEventListener> users = listeners.get(eventType);
        for (CampaignEventListener listener : users) {
            listener.update(eventType, campaignId, details);
        }
    }
}
