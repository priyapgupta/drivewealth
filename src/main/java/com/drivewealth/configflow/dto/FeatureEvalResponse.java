package com.drivewealth.configflow.dto;

public class FeatureEvalResponse {
    private String key;
    private boolean enabled;
    private int rolloutPercent;
    private String value;

    public FeatureEvalResponse() {}

    public FeatureEvalResponse(String key, boolean enabled, int rolloutPercent, String value) {
        this.key = key;
        this.enabled = enabled;
        this.rolloutPercent = rolloutPercent;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRolloutPercent() {
        return rolloutPercent;
    }

    public void setRolloutPercent(int rolloutPercent) {
        this.rolloutPercent = rolloutPercent;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

