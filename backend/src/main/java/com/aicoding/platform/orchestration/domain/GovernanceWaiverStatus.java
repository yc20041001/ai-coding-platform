package com.aicoding.platform.orchestration.domain;

public enum GovernanceWaiverStatus {
    REQUESTED("REQUESTED"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    EXPIRED("EXPIRED"),
    REVOKED("REVOKED");

    private final String value;

    GovernanceWaiverStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
