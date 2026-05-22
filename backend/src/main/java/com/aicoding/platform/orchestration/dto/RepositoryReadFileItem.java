package com.aicoding.platform.orchestration.dto;

public class RepositoryReadFileItem {

    private String filePath;
    private String language;
    private Long fileSize;
    private Integer lineCount;
    private String contentHash;
    private Boolean truncated;
    private Boolean redacted;

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Integer getLineCount() { return lineCount; }
    public void setLineCount(Integer lineCount) { this.lineCount = lineCount; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public Boolean getTruncated() { return truncated; }
    public void setTruncated(Boolean truncated) { this.truncated = truncated; }

    public Boolean getRedacted() { return redacted; }
    public void setRedacted(Boolean redacted) { this.redacted = redacted; }
}
