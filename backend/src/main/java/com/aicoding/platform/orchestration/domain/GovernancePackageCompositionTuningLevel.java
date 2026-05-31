package com.aicoding.platform.orchestration.domain;

public enum GovernancePackageCompositionTuningLevel {
    ADD_SECTION("ADD_SECTION"), REMOVE_SECTION("REMOVE_SECTION"),
    REORDER("REORDER"), HIGHLIGHT("HIGHLIGHT"), DEFAULT("DEFAULT");
    private final String value;
    GovernancePackageCompositionTuningLevel(String v) { this.value = v; }
    public String getValue() { return value; }
}
