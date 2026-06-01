package com.aicoding.platform.orchestration.domain;

public enum GovernanceBenchmarkAdoptionStatus {
    IDENTIFIED("IDENTIFIED"), IN_PROGRESS("IN_PROGRESS"), ADOPTED("ADOPTED"), BLOCKED("BLOCKED"), NOT_APPLICABLE("NOT_APPLICABLE");
    private final String value;
    GovernanceBenchmarkAdoptionStatus(String v) { this.value = v; }
    public String getValue() { return value; }
}
