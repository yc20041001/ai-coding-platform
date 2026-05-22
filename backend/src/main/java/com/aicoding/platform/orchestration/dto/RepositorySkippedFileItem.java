package com.aicoding.platform.orchestration.dto;

public class RepositorySkippedFileItem {

    private String filePath;
    private String reason;

    public RepositorySkippedFileItem() {}

    public RepositorySkippedFileItem(String filePath, String reason) {
        this.filePath = filePath;
        this.reason = reason;
    }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
