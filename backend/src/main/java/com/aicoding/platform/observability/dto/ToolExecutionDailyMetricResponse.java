package com.aicoding.platform.observability.dto;

public class ToolExecutionDailyMetricResponse {

    private String date;
    private Long totalJobs;
    private Long completedJobs;
    private Long failedJobs;
    private Long deadLetteredJobs;
    private Long retryPendingJobs;
    private Double avgDurationMs;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Long getTotalJobs() { return totalJobs; }
    public void setTotalJobs(Long totalJobs) { this.totalJobs = totalJobs; }

    public Long getCompletedJobs() { return completedJobs; }
    public void setCompletedJobs(Long completedJobs) { this.completedJobs = completedJobs; }

    public Long getFailedJobs() { return failedJobs; }
    public void setFailedJobs(Long failedJobs) { this.failedJobs = failedJobs; }

    public Long getDeadLetteredJobs() { return deadLetteredJobs; }
    public void setDeadLetteredJobs(Long deadLetteredJobs) { this.deadLetteredJobs = deadLetteredJobs; }

    public Long getRetryPendingJobs() { return retryPendingJobs; }
    public void setRetryPendingJobs(Long retryPendingJobs) { this.retryPendingJobs = retryPendingJobs; }

    public Double getAvgDurationMs() { return avgDurationMs; }
    public void setAvgDurationMs(Double avgDurationMs) { this.avgDurationMs = avgDurationMs; }
}
