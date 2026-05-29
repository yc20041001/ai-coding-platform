package com.aicoding.platform.orchestration.domain;

public enum ReleaseEvidenceBundleStatus {

    DRAFT("DRAFT"),
    GENERATED("GENERATED"),
    PUBLISHED("PUBLISHED"),
    ARCHIVED("ARCHIVED");

    private final String value;

    ReleaseEvidenceBundleStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
