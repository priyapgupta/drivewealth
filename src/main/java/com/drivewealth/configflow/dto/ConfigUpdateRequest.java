package com.drivewealth.configflow.dto;

public class ConfigUpdateRequest {
    private String value;
    private Integer rolloutPercent = 0;
    public String updatedBy;

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public Integer getRolloutPercent() { return rolloutPercent; }
    public void setRolloutPercent(Integer rolloutPercent) { this.rolloutPercent = rolloutPercent; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
