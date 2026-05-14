package com.aicoding.platform.repository.dto;

public class RepositoryBranchResponse {

    private String name;
    private String commitHash;
    private boolean protectedBranch;
    private String lastSyncTime;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }

    public boolean isProtectedBranch() { return protectedBranch; }
    public void setProtectedBranch(boolean protectedBranch) { this.protectedBranch = protectedBranch; }

    public String getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(String lastSyncTime) { this.lastSyncTime = lastSyncTime; }
}
