package com.aicoding.platform.orchestration.domain;

public enum PortfolioDriftLevel {
    STABLE("STABLE"),
    WATCH("WATCH"),
    HIGH("HIGH"),
    CRITICAL("CRITICAL");

    private final String value;

    PortfolioDriftLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
