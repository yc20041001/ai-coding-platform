package com.aicoding.platform.orchestration.domain;

public enum ReleaseVerificationPhase {

    PRE_RELEASE("PRE_RELEASE"),
    ROLLOUT("ROLLOUT"),
    OBSERVATION("OBSERVATION"),
    POST_RELEASE("POST_RELEASE");

    private final String value;

    ReleaseVerificationPhase(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
