package com.aicoding.platform.observability.dto;

public class ModelUsageDailyResponse {

    private String date;
    private Long requestCount;
    private Long totalTokens;
    private Long successCount;
    private Long failureCount;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Long getRequestCount() { return requestCount; }
    public void setRequestCount(Long requestCount) { this.requestCount = requestCount; }

    public Long getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Long totalTokens) { this.totalTokens = totalTokens; }

    public Long getSuccessCount() { return successCount; }
    public void setSuccessCount(Long successCount) { this.successCount = successCount; }

    public Long getFailureCount() { return failureCount; }
    public void setFailureCount(Long failureCount) { this.failureCount = failureCount; }
}
