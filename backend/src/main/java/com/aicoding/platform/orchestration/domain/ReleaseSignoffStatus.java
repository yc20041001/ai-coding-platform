package com.aicoding.platform.orchestration.domain;

public enum ReleaseSignoffStatus {

    PENDING("PENDING"),
    APPROVED("APPROVED"),
    CONDITIONAL("CONDITIONAL"),
    REJECTED("REJECTED"),
    SKIPPED("SKIPPED");

    private final String value;

    ReleaseSignoffStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
