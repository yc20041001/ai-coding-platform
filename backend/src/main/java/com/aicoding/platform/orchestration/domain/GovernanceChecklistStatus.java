package com.aicoding.platform.orchestration.domain;

public enum GovernanceChecklistStatus {
    OPEN("OPEN"), IN_PROGRESS("IN_PROGRESS"), COMPLETED("COMPLETED"), CANCELLED("CANCELLED");
    private final String value;
    GovernanceChecklistStatus(String v) { this.value = v; }
    public String getValue() { return value; }
}
