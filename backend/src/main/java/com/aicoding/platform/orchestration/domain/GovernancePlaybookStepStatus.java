package com.aicoding.platform.orchestration.domain;

public enum GovernancePlaybookStepStatus {
    TODO("TODO"), DOING("DOING"), DONE("DONE"), SKIPPED("SKIPPED"), BLOCKED("BLOCKED");
    private final String value;
    GovernancePlaybookStepStatus(String v) { this.value = v; }
    public String getValue() { return value; }
}
