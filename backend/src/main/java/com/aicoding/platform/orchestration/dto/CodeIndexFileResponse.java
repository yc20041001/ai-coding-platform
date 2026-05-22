package com.aicoding.platform.orchestration.dto;

public class CodeIndexFileResponse {

    private String id;
    private String projectId;
    private String filePath;
    private String language;
    private Long fileSize;
    private Integer lineCount;
    private String status;
    private String indexedAt;

    public CodeIndexFileResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Integer getLineCount() { return lineCount; }
    public void setLineCount(Integer lineCount) { this.lineCount = lineCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIndexedAt() { return indexedAt; }
    public void setIndexedAt(String indexedAt) { this.indexedAt = indexedAt; }
}
