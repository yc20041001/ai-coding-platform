package com.aicoding.platform.orchestration.domain;

public enum GovernanceBenchmarkWindow {
    QUARTER(90), MONTH(30), WEEK(7);
    private final int days;
    GovernanceBenchmarkWindow(int days) { this.days = days; }
    public int getDays() { return days; }
    public String getValue() { return name(); }
}
