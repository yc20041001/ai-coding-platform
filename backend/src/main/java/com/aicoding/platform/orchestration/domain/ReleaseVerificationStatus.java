package com.aicoding.platform.orchestration.domain;

public enum ReleaseVerificationStatus {

    PASS("PASS"),
    WARN("WARN"),
    FAIL("FAIL"),
    SKIP("SKIP");

    private final String value;

    ReleaseVerificationStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
