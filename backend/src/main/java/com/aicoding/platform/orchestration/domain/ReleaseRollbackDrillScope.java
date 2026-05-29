package com.aicoding.platform.orchestration.domain;

public enum ReleaseRollbackDrillScope {

    CONFIG_ONLY("CONFIG_ONLY"),
    APP_VERSION("APP_VERSION"),
    DB_AND_APP("DB_AND_APP"),
    FULL_ENVIRONMENT("FULL_ENVIRONMENT");

    private final String value;

    ReleaseRollbackDrillScope(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
