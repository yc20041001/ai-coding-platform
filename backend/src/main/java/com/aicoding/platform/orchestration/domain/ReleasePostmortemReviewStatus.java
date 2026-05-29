package com.aicoding.platform.orchestration.domain;

public enum ReleasePostmortemReviewStatus {

    DRAFT("DRAFT"),
    REVIEWED("REVIEWED"),
    PUBLISHED("PUBLISHED"),
    ARCHIVED("ARCHIVED");

    private final String value;

    ReleasePostmortemReviewStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
