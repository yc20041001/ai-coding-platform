package com.aicoding.platform.orchestration.domain;

public enum GovernanceBacklogHealthLevel {
    HEALTHY("HEALTHY"), WATCH("WATCH"), RISK("RISK"), CRITICAL("CRITICAL");
    private final String value;
    GovernanceBacklogHealthLevel(String v) { this.value = v; }
    public String getValue() { return value; }
}
