package com.aicoding.platform.orchestration.domain;

public enum OwnerHealthLevel {
    HEALTHY("HEALTHY"), WATCH("WATCH"), RISK("RISK"), CRITICAL("CRITICAL");
    private final String value;
    OwnerHealthLevel(String value) { this.value = value; }
    public String getValue() { return value; }
}
