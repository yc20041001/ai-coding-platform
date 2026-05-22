package com.aicoding.platform.orchestration.dto;

import java.util.List;

public class ReadOnlyRepositoryRequest {

    private String branch;
    private String baseBranch;
    private String pathPrefix;
    private String filePath;
    private Integer startLine;
    private Integer maxLines;
    private Integer maxFiles;
    private Long maxBytes;
    private boolean includeRemote;
    private List<String> allowPrefixes;

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getBaseBranch() { return baseBranch; }
    public void setBaseBranch(String baseBranch) { this.baseBranch = baseBranch; }

    public String getPathPrefix() { return pathPrefix; }
    public void setPathPrefix(String pathPrefix) { this.pathPrefix = pathPrefix; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Integer getStartLine() { return startLine; }
    public void setStartLine(Integer startLine) { this.startLine = startLine; }

    public Integer getMaxLines() { return maxLines; }
    public void setMaxLines(Integer maxLines) { this.maxLines = maxLines; }

    public Integer getMaxFiles() { return maxFiles; }
    public void setMaxFiles(Integer maxFiles) { this.maxFiles = maxFiles; }

    public Long getMaxBytes() { return maxBytes; }
    public void setMaxBytes(Long maxBytes) { this.maxBytes = maxBytes; }

    public boolean isIncludeRemote() { return includeRemote; }
    public void setIncludeRemote(boolean includeRemote) { this.includeRemote = includeRemote; }

    public List<String> getAllowPrefixes() { return allowPrefixes; }
    public void setAllowPrefixes(List<String> allowPrefixes) { this.allowPrefixes = allowPrefixes; }
}
