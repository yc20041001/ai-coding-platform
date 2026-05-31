package com.aicoding.platform.orchestration.domain;

public enum GovernanceRecommendationPackageStatus {
    DRAFT("DRAFT"), READY("READY"), REVIEWED("REVIEWED"), ARCHIVED("ARCHIVED");
    private final String value;
    GovernanceRecommendationPackageStatus(String v) { this.value = v; }
    public String getValue() { return value; }
}
