package com.aicoding.platform.observability.dto;

public class ToolExecutionSummaryResponse {

    private Long totalJobs;
    private Long pendingJobs;
    private Long runningJobs;
    private Long completedJobs;
    private Long failedJobs;
    private Long retryPendingJobs;
    private Long canceledJobs;
    private Long deadLetteredJobs;
    private Double successRate;
    private Double failureRate;
    private Double retryRate;
    private Double avgDurationMs;
    private Long maxDurationMs;
    private Long totalRetries;

    public Long getTotalJobs() { return totalJobs; }
    public void setTotalJobs(Long totalJobs) { this.totalJobs = totalJobs; }

    public Long getPendingJobs() { return pendingJobs; }
    public void setPendingJobs(Long pendingJobs) { this.pendingJobs = pendingJobs; }

    public Long getRunningJobs() { return runningJobs; }
    public void setRunningJobs(Long runningJobs) { this.runningJobs = runningJobs; }

    public Long getCompletedJobs() { return completedJobs; }
    public void setCompletedJobs(Long completedJobs) { this.completedJobs = completedJobs; }

    public Long getFailedJobs() { return failedJobs; }
    public void setFailedJobs(Long failedJobs) { this.failedJobs = failedJobs; }

    public Long getRetryPendingJobs() { return retryPendingJobs; }
    public void setRetryPendingJobs(Long retryPendingJobs) { this.retryPendingJobs = retryPendingJobs; }

    public Long getCanceledJobs() { return canceledJobs; }
    public void setCanceledJobs(Long canceledJobs) { this.canceledJobs = canceledJobs; }

    public Long getDeadLetteredJobs() { return deadLetteredJobs; }
    public void setDeadLetteredJobs(Long deadLetteredJobs) { this.deadLetteredJobs = deadLetteredJobs; }

    public Double getSuccessRate() { return successRate; }
    public void setSuccessRate(Double successRate) { this.successRate = successRate; }

    public Double getFailureRate() { return failureRate; }
    public void setFailureRate(Double failureRate) { this.failureRate = failureRate; }

    public Double getRetryRate() { return retryRate; }
    public void setRetryRate(Double retryRate) { this.retryRate = retryRate; }

    public Double getAvgDurationMs() { return avgDurationMs; }
    public void setAvgDurationMs(Double avgDurationMs) { this.avgDurationMs = avgDurationMs; }

    public Long getMaxDurationMs() { return maxDurationMs; }
    public void setMaxDurationMs(Long maxDurationMs) { this.maxDurationMs = maxDurationMs; }

    public Long getTotalRetries() { return totalRetries; }
    public void setTotalRetries(Long totalRetries) { this.totalRetries = totalRetries; }
}
