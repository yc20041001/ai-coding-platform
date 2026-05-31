package com.aicoding.platform.orchestration.domain;

public enum GovernanceGuidedTaskStatus {
    OPEN("OPEN"), IN_PROGRESS("IN_PROGRESS"), DONE("DONE"), SKIPPED("SKIPPED"), BLOCKED("BLOCKED");
    private final String value;
    GovernanceGuidedTaskStatus(String v) { this.value = v; }
    public String getValue() { return value; }
}
