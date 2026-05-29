package com.aicoding.platform.orchestration.domain;

public enum ReleaseRolloutStatus {

    DRAFT("DRAFT"),
    READY("READY"),
    IN_PROGRESS("IN_PROGRESS"),
    OBSERVING("OBSERVING"),
    COMPLETED("COMPLETED"),
    ROLLED_BACK("ROLLED_BACK"),
    CANCELLED("CANCELLED");

    private final String value;

    ReleaseRolloutStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
