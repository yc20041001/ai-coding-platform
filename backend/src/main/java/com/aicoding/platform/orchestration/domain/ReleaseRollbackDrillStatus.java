package com.aicoding.platform.orchestration.domain;

public enum ReleaseRollbackDrillStatus {

    PLANNED("PLANNED"),
    RUNNING("RUNNING"),
    PASSED("PASSED"),
    FAILED("FAILED"),
    BLOCKED("BLOCKED"),
    CANCELLED("CANCELLED");

    private final String value;

    ReleaseRollbackDrillStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
