package com.givinghands.givinghands.notification;

import java.util.HashMap;
import java.util.Map;

public class TemplateContext {

    private final Map<String, Object> values = new HashMap<>();

    public TemplateContext put(String key, Object value) {
        values.put(key, value);
        return this;
    }

    public Map<String, Object> asMap() {
        return values;
    }
}

