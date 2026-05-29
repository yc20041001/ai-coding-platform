package com.aicoding.platform.orchestration.domain;

public enum ReleaseRolloutStepStatus {

    PENDING("PENDING"),
    RUNNING("RUNNING"),
    PASSED("PASSED"),
    FAILED("FAILED"),
    SKIPPED("SKIPPED"),
    BLOCKED("BLOCKED");

    private final String value;

    ReleaseRolloutStepStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
