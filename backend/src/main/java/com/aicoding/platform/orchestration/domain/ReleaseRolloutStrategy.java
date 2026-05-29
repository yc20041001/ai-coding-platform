package com.aicoding.platform.orchestration.domain;

public enum ReleaseRolloutStrategy {

    MANUAL_FULL("MANUAL_FULL"),
    PHASED_PERCENTAGE("PHASED_PERCENTAGE"),
    INTERNAL_ONLY("INTERNAL_ONLY"),
    PROJECT_WHITELIST("PROJECT_WHITELIST");

    private final String value;

    ReleaseRolloutStrategy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
