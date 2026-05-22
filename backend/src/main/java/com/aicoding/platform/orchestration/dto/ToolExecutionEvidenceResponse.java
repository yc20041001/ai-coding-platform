package com.aicoding.platform.orchestration.dto;

import java.util.List;

public class ToolExecutionEvidenceResponse {

    private Integer filesReadCount;
    private Integer skippedFilesCount;
    private Boolean redacted;
    private Boolean truncated;
    private Boolean binarySkipped;
    private Boolean pathSandboxApplied;
    private Boolean sensitiveDenylistApplied;
    private List<ToolExecutionFileEvidenceResponse> filesRead;
    private List<ToolExecutionFileEvidenceResponse> skippedFiles;
    private List<ToolExecutionArtifactEvidenceResponse> artifacts;

    public ToolExecutionEvidenceResponse() {}

    public Integer getFilesReadCount() { return filesReadCount; }
    public void setFilesReadCount(Integer filesReadCount) { this.filesReadCount = filesReadCount; }

    public Integer getSkippedFilesCount() { return skippedFilesCount; }
    public void setSkippedFilesCount(Integer skippedFilesCount) { this.skippedFilesCount = skippedFilesCount; }

    public Boolean getRedacted() { return redacted; }
    public void setRedacted(Boolean redacted) { this.redacted = redacted; }

    public Boolean getTruncated() { return truncated; }
    public void setTruncated(Boolean truncated) { this.truncated = truncated; }

    public Boolean getBinarySkipped() { return binarySkipped; }
    public void setBinarySkipped(Boolean binarySkipped) { this.binarySkipped = binarySkipped; }

    public Boolean getPathSandboxApplied() { return pathSandboxApplied; }
    public void setPathSandboxApplied(Boolean pathSandboxApplied) { this.pathSandboxApplied = pathSandboxApplied; }

    public Boolean getSensitiveDenylistApplied() { return sensitiveDenylistApplied; }
    public void setSensitiveDenylistApplied(Boolean sensitiveDenylistApplied) { this.sensitiveDenylistApplied = sensitiveDenylistApplied; }

    public List<ToolExecutionFileEvidenceResponse> getFilesRead() { return filesRead; }
    public void setFilesRead(List<ToolExecutionFileEvidenceResponse> filesRead) { this.filesRead = filesRead; }

    public List<ToolExecutionFileEvidenceResponse> getSkippedFiles() { return skippedFiles; }
    public void setSkippedFiles(List<ToolExecutionFileEvidenceResponse> skippedFiles) { this.skippedFiles = skippedFiles; }

    public List<ToolExecutionArtifactEvidenceResponse> getArtifacts() { return artifacts; }
    public void setArtifacts(List<ToolExecutionArtifactEvidenceResponse> artifacts) { this.artifacts = artifacts; }
}
