package com.aicoding.platform.orchestration.domain;

public enum GovernanceCampaignEffectivenessLevel {
    HIGH("HIGH"), MEDIUM("MEDIUM"), LOW("LOW"), NONE("NONE");
    private final String value;
    GovernanceCampaignEffectivenessLevel(String v) { this.value = v; }
    public String getValue() { return value; }
}
