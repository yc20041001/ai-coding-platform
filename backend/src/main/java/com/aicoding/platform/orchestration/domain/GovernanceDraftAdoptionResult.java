package com.aicoding.platform.orchestration.domain;

public enum GovernanceDraftAdoptionResult {
    ADOPTED("ADOPTED"), MODIFIED_AND_ADOPTED("MODIFIED_AND_ADOPTED"), REJECTED("REJECTED"), SUPERSEDED("SUPERSEDED");
    private final String value;
    GovernanceDraftAdoptionResult(String v) { this.value = v; }
    public String getValue() { return value; }
}
