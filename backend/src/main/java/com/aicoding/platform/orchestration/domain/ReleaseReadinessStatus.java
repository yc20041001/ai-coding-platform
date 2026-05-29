package com.aicoding.platform.orchestration.domain;

public enum ReleaseReadinessStatus {

    READY("READY"),
    READY_WITH_RISK("READY_WITH_RISK"),
    NOT_READY("NOT_READY");

    private final String value;

    ReleaseReadinessStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
