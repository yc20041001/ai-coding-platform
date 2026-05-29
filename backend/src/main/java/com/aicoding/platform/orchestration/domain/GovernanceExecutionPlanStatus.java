package com.aicoding.platform.orchestration.domain;

public enum GovernanceExecutionPlanStatus {
    DRAFT("DRAFT"), READY("READY"), IN_PROGRESS("IN_PROGRESS"), BLOCKED("BLOCKED"), COMPLETED("COMPLETED"), ARCHIVED("ARCHIVED");
    private final String value;
    GovernanceExecutionPlanStatus(String v) { this.value = v; }
    public String getValue() { return value; }
}
