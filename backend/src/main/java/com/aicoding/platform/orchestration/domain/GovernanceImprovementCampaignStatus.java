package com.aicoding.platform.orchestration.domain;

public enum GovernanceImprovementCampaignStatus {
    DRAFT("DRAFT"), ACTIVE("ACTIVE"), COMPLETED("COMPLETED"), CANCELLED("CANCELLED");
    private final String value;
    GovernanceImprovementCampaignStatus(String v) { this.value = v; }
    public String getValue() { return value; }
}
