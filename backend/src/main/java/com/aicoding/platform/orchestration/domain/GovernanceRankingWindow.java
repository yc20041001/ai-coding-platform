package com.aicoding.platform.orchestration.domain;

public enum GovernanceRankingWindow {
    LAST_QUARTER(90), LAST_MONTH(30), LAST_WEEK(7);
    private final int days;
    GovernanceRankingWindow(int days) { this.days = days; }
    public int getDays() { return days; }
    public String getValue() { return name(); }
}
