package com.aicoding.platform.orchestration.domain;

public enum GovernanceEscalationLevel {
    INFO("INFO"), LOW("LOW"), MEDIUM("MEDIUM"), HIGH("HIGH"), CRITICAL("CRITICAL");
    private final String value;
    GovernanceEscalationLevel(String value) { this.value = value; }
    public String getValue() { return value; }
}
