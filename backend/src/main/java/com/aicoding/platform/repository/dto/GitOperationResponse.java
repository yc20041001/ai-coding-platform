package com.aicoding.platform.repository.dto;

public class GitOperationResponse {

    private String operationId;
    private String status;
    private String commitHash;
    private String prUrl;

    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }

    public String getPrUrl() { return prUrl; }
    public void setPrUrl(String prUrl) { this.prUrl = prUrl; }
}
