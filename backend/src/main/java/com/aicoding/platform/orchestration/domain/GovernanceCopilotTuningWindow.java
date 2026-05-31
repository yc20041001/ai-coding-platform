package com.aicoding.platform.orchestration.domain;

public enum GovernanceCopilotTuningWindow {
    DAY_7("DAY_7"), DAY_14("DAY_14"), DAY_30("DAY_30");
    private final String value;
    GovernanceCopilotTuningWindow(String v) { this.value = v; }
    public String getValue() { return value; }
}
