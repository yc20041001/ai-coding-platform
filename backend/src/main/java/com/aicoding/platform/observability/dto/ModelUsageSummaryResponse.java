package com.aicoding.platform.observability.dto;

public class ModelUsageSummaryResponse {

    private Long requestCount;
    private Long successCount;
    private Long failureCount;
    private java.math.BigDecimal successRate;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private java.math.BigDecimal avgLatencyMs;
    private Long mockCount;
    private Long realProviderCount;

    public Long getRequestCount() { return requestCount; }
    public void setRequestCount(Long requestCount) { this.requestCount = requestCount; }

    public Long getSuccessCount() { return successCount; }
    public void setSuccessCount(Long successCount) { this.successCount = successCount; }

    public Long getFailureCount() { return failureCount; }
    public void setFailureCount(Long failureCount) { this.failureCount = failureCount; }

    public java.math.BigDecimal getSuccessRate() { return successRate; }
    public void setSuccessRate(java.math.BigDecimal successRate) { this.successRate = successRate; }

    public Long getPromptTokens() { return promptTokens; }
    public void setPromptTokens(Long promptTokens) { this.promptTokens = promptTokens; }

    public Long getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(Long completionTokens) { this.completionTokens = completionTokens; }

    public Long getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Long totalTokens) { this.totalTokens = totalTokens; }

    public java.math.BigDecimal getAvgLatencyMs() { return avgLatencyMs; }
    public void setAvgLatencyMs(java.math.BigDecimal avgLatencyMs) { this.avgLatencyMs = avgLatencyMs; }

    public Long getMockCount() { return mockCount; }
    public void setMockCount(Long mockCount) { this.mockCount = mockCount; }

    public Long getRealProviderCount() { return realProviderCount; }
    public void setRealProviderCount(Long realProviderCount) { this.realProviderCount = realProviderCount; }
}
