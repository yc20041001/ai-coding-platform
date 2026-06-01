package com.aicoding.platform.orchestration.domain;

public enum GovernanceImprovementWindow {
    MONTH_1(30), MONTH_3(90), MONTH_6(180);
    private final int days;
    GovernanceImprovementWindow(int days) { this.days = days; }
    public int getDays() { return days; }
    public String getValue() { return name(); }
}
