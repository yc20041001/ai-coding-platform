package com.aicoding.platform.observability.dto;

public class ToolExecutionToolMetricResponse {

    private String toolKey;
    private Long totalJobs;
    private Long completedJobs;
    private Long failedJobs;
    private Long deadLetteredJobs;
    private Double successRate;
    private Double avgDurationMs;
    private Long totalRetries;
    private String topErrorCode;
    private String topFailureStage;

    public String getToolKey() { return toolKey; }
    public void setToolKey(String toolKey) { this.toolKey = toolKey; }

    public Long getTotalJobs() { return totalJobs; }
    public void setTotalJobs(Long totalJobs) { this.totalJobs = totalJobs; }

    public Long getCompletedJobs() { return completedJobs; }
    public void setCompletedJobs(Long completedJobs) { this.completedJobs = completedJobs; }

    public Long getFailedJobs() { return failedJobs; }
    public void setFailedJobs(Long failedJobs) { this.failedJobs = failedJobs; }

    public Long getDeadLetteredJobs() { return deadLetteredJobs; }
    public void setDeadLetteredJobs(Long deadLetteredJobs) { this.deadLetteredJobs = deadLetteredJobs; }

    public Double getSuccessRate() { return successRate; }
    public void setSuccessRate(Double successRate) { this.successRate = successRate; }

    public Double getAvgDurationMs() { return avgDurationMs; }
    public void setAvgDurationMs(Double avgDurationMs) { this.avgDurationMs = avgDurationMs; }

    public Long getTotalRetries() { return totalRetries; }
    public void setTotalRetries(Long totalRetries) { this.totalRetries = totalRetries; }

    public String getTopErrorCode() { return topErrorCode; }
    public void setTopErrorCode(String topErrorCode) { this.topErrorCode = topErrorCode; }

    public String getTopFailureStage() { return topFailureStage; }
    public void setTopFailureStage(String topFailureStage) { this.topFailureStage = topFailureStage; }
}
