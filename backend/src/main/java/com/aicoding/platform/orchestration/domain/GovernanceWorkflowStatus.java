package com.aicoding.platform.orchestration.domain;

public enum GovernanceWorkflowStatus {
    OPEN("OPEN"),
    ACKNOWLEDGED("ACKNOWLEDGED"),
    IN_PROGRESS("IN_PROGRESS"),
    BLOCKED("BLOCKED"),
    COMPLETED("COMPLETED"),
    REJECTED("REJECTED");

    private final String value;

    GovernanceWorkflowStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
