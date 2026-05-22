package com.aicoding.platform.orchestration.dto;

import java.util.List;

public class RepositoryDiffSummaryResult {

    private String baseBranch;
    private String targetBranch;
    private int fileCount;
    private int additionCount;
    private int deletionCount;
    private List<String> changedFiles;
    private boolean truncated;
    private boolean noRealGitDiff;

    public String getBaseBranch() { return baseBranch; }
    public void setBaseBranch(String baseBranch) { this.baseBranch = baseBranch; }

    public String getTargetBranch() { return targetBranch; }
    public void setTargetBranch(String targetBranch) { this.targetBranch = targetBranch; }

    public int getFileCount() { return fileCount; }
    public void setFileCount(int fileCount) { this.fileCount = fileCount; }

    public int getAdditionCount() { return additionCount; }
    public void setAdditionCount(int additionCount) { this.additionCount = additionCount; }

    public int getDeletionCount() { return deletionCount; }
    public void setDeletionCount(int deletionCount) { this.deletionCount = deletionCount; }

    public List<String> getChangedFiles() { return changedFiles; }
    public void setChangedFiles(List<String> changedFiles) { this.changedFiles = changedFiles; }

    public boolean isTruncated() { return truncated; }
    public void setTruncated(boolean truncated) { this.truncated = truncated; }

    public boolean isNoRealGitDiff() { return noRealGitDiff; }
    public void setNoRealGitDiff(boolean noRealGitDiff) { this.noRealGitDiff = noRealGitDiff; }
}
