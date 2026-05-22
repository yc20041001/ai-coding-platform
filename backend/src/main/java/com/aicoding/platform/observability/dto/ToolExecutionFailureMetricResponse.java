package com.aicoding.platform.observability.dto;

public class ToolExecutionFailureMetricResponse {

    private String errorCode;
    private Long count;
    private String latestTime;

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }

    public String getLatestTime() { return latestTime; }
    public void setLatestTime(String latestTime) { this.latestTime = latestTime; }
}
