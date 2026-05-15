package com.aicoding.platform.modelgateway.dto;

import java.math.BigDecimal;
import java.util.List;

public class ModelUsageCostResponse {

    private long totalRequests;
    private long successCount;
    private long failureCount;
    private long fallbackCount;
    private double successRate;
    private long totalTokens;
    private long promptTokens;
    private long completionTokens;
    private BigDecimal estimatedCost;
    private List<ProviderBreakdown> providerBreakdowns;
    private List<ModelBreakdown> modelBreakdowns;

    public long getTotalRequests() { return totalRequests; }
    public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }

    public long getSuccessCount() { return successCount; }
    public void setSuccessCount(long successCount) { this.successCount = successCount; }

    public long getFailureCount() { return failureCount; }
    public void setFailureCount(long failureCount) { this.failureCount = failureCount; }

    public long getFallbackCount() { return fallbackCount; }
    public void setFallbackCount(long fallbackCount) { this.fallbackCount = fallbackCount; }

    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }

    public long getTotalTokens() { return totalTokens; }
    public void setTotalTokens(long totalTokens) { this.totalTokens = totalTokens; }

    public long getPromptTokens() { return promptTokens; }
    public void setPromptTokens(long promptTokens) { this.promptTokens = promptTokens; }

    public long getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(long completionTokens) { this.completionTokens = completionTokens; }

    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }

    public List<ProviderBreakdown> getProviderBreakdowns() { return providerBreakdowns; }
    public void setProviderBreakdowns(List<ProviderBreakdown> providerBreakdowns) { this.providerBreakdowns = providerBreakdowns; }

    public List<ModelBreakdown> getModelBreakdowns() { return modelBreakdowns; }
    public void setModelBreakdowns(List<ModelBreakdown> modelBreakdowns) { this.modelBreakdowns = modelBreakdowns; }

    public static class ProviderBreakdown {
        private String provider;
        private long requestCount;
        private long successCount;
        private long tokenCount;
        private BigDecimal cost;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public long getRequestCount() { return requestCount; }
        public void setRequestCount(long requestCount) { this.requestCount = requestCount; }

        public long getSuccessCount() { return successCount; }
        public void setSuccessCount(long successCount) { this.successCount = successCount; }

        public long getTokenCount() { return tokenCount; }
        public void setTokenCount(long tokenCount) { this.tokenCount = tokenCount; }

        public BigDecimal getCost() { return cost; }
        public void setCost(BigDecimal cost) { this.cost = cost; }
    }

    public static class ModelBreakdown {
        private String modelName;
        private long requestCount;
        private long tokenCount;
        private BigDecimal cost;

        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }

        public long getRequestCount() { return requestCount; }
        public void setRequestCount(long requestCount) { this.requestCount = requestCount; }

        public long getTokenCount() { return tokenCount; }
        public void setTokenCount(long tokenCount) { this.tokenCount = tokenCount; }

        public BigDecimal getCost() { return cost; }
        public void setCost(BigDecimal cost) { this.cost = cost; }
    }
}
