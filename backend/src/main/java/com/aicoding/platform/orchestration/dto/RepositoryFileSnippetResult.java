package com.aicoding.platform.orchestration.dto;

import java.util.List;

public class RepositoryFileSnippetResult {

    private String filePath;
    private String branch;
    private String content;
    private String language;
    private Integer startLine;
    private Integer endLine;
    private Integer totalLines;
    private boolean truncated;
    private boolean redacted;
    private int redactionCount;
    private List<RepositorySkippedFileItem> skippedFiles;

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Integer getStartLine() { return startLine; }
    public void setStartLine(Integer startLine) { this.startLine = startLine; }

    public Integer getEndLine() { return endLine; }
    public void setEndLine(Integer endLine) { this.endLine = endLine; }

    public Integer getTotalLines() { return totalLines; }
    public void setTotalLines(Integer totalLines) { this.totalLines = totalLines; }

    public boolean isTruncated() { return truncated; }
    public void setTruncated(boolean truncated) { this.truncated = truncated; }

    public boolean isRedacted() { return redacted; }
    public void setRedacted(boolean redacted) { this.redacted = redacted; }

    public int getRedactionCount() { return redactionCount; }
    public void setRedactionCount(int redactionCount) { this.redactionCount = redactionCount; }

    public List<RepositorySkippedFileItem> getSkippedFiles() { return skippedFiles; }
    public void setSkippedFiles(List<RepositorySkippedFileItem> skippedFiles) { this.skippedFiles = skippedFiles; }
}
