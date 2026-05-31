package com.aicoding.platform.orchestration.domain;

public enum GovernanceBenchmarkSignalLevel {
    POSITIVE("POSITIVE"), NEUTRAL("NEUTRAL"), NEGATIVE("NEGATIVE"), INSUFFICIENT("INSUFFICIENT");
    private final String value;
    GovernanceBenchmarkSignalLevel(String v) { this.value = v; }
    public String getValue() { return value; }
}
