package com.aicoding.platform.orchestration.domain;

public enum GovernanceDraftModificationLevel {
    NONE("NONE"), MINOR("MINOR"), MODERATE("MODERATE"), SIGNIFICANT("SIGNIFICANT");
    private final String value;
    GovernanceDraftModificationLevel(String v) { this.value = v; }
    public String getValue() { return value; }
}
