package com.aicoding.platform.observability.dto;

import java.util.List;

public class ToolExecutionMetricsResponse {

    private ToolExecutionSummaryResponse summary;
    private List<ToolExecutionToolMetricResponse> tools;
    private List<ToolExecutionDailyMetricResponse> daily;
    private List<ToolExecutionFailureMetricResponse> errorCodes;
    private List<ToolExecutionFailureMetricResponse> failureStages;

    public ToolExecutionSummaryResponse getSummary() { return summary; }
    public void setSummary(ToolExecutionSummaryResponse summary) { this.summary = summary; }

    public List<ToolExecutionToolMetricResponse> getTools() { return tools; }
    public void setTools(List<ToolExecutionToolMetricResponse> tools) { this.tools = tools; }

    public List<ToolExecutionDailyMetricResponse> getDaily() { return daily; }
    public void setDaily(List<ToolExecutionDailyMetricResponse> daily) { this.daily = daily; }

    public List<ToolExecutionFailureMetricResponse> getErrorCodes() { return errorCodes; }
    public void setErrorCodes(List<ToolExecutionFailureMetricResponse> errorCodes) { this.errorCodes = errorCodes; }

    public List<ToolExecutionFailureMetricResponse> getFailureStages() { return failureStages; }
    public void setFailureStages(List<ToolExecutionFailureMetricResponse> failureStages) { this.failureStages = failureStages; }
}
