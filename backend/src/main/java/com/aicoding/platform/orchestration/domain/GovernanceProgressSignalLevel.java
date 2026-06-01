package com.aicoding.platform.orchestration.domain;

public enum GovernanceProgressSignalLevel {
    ON_TRACK("ON_TRACK"), AT_RISK("AT_RISK"), BEHIND("BEHIND"), NOT_STARTED("NOT_STARTED");
    private final String value;
    GovernanceProgressSignalLevel(String v) { this.value = v; }
    public String getValue() { return value; }
}
