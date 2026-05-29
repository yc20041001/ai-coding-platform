package com.aicoding.platform.orchestration.domain;

public enum ReleaseRiskCategory {
    INCIDENT("INCIDENT"),
    ALERT("ALERT"),
    VERIFICATION("VERIFICATION"),
    ROLLOUT("ROLLOUT"),
    SIGNOFF("SIGNOFF"),
    COST("COST"),
    PR_QUALITY("PR_QUALITY");

    private final String value;

    ReleaseRiskCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
