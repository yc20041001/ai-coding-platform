package com.aicoding.platform.orchestration.dto;

import java.util.List;

public class RepositoryTreeResult {

    private String branch;
    private String pathPrefix;
    private List<RepositoryReadFileItem> files;
    private List<RepositorySkippedFileItem> skippedFiles;
    private boolean truncated;
    private boolean redacted;

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getPathPrefix() { return pathPrefix; }
    public void setPathPrefix(String pathPrefix) { this.pathPrefix = pathPrefix; }

    public List<RepositoryReadFileItem> getFiles() { return files; }
    public void setFiles(List<RepositoryReadFileItem> files) { this.files = files; }

    public List<RepositorySkippedFileItem> getSkippedFiles() { return skippedFiles; }
    public void setSkippedFiles(List<RepositorySkippedFileItem> skippedFiles) { this.skippedFiles = skippedFiles; }

    public boolean isTruncated() { return truncated; }
    public void setTruncated(boolean truncated) { this.truncated = truncated; }

    public boolean isRedacted() { return redacted; }
    public void setRedacted(boolean redacted) { this.redacted = redacted; }
}
