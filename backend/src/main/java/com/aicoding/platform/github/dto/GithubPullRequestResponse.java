package com.aicoding.platform.github.dto;

public class GithubPullRequestResponse {
    private String id;
    private Long githubPrId;
    private Long githubRepoId;
    private Integer number;
    private String title;
    private String state;
    private String authorLogin;
    private String baseBranch;
    private String headBranch;
    private String htmlUrl;
    private Integer additions;
    private Integer deletions;
    private Integer changedFiles;
    private String githubCreatedAt;
    private String githubUpdatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getGithubPrId() { return githubPrId; }
    public void setGithubPrId(Long githubPrId) { this.githubPrId = githubPrId; }

    public Long getGithubRepoId() { return githubRepoId; }
    public void setGithubRepoId(Long githubRepoId) { this.githubRepoId = githubRepoId; }

    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getAuthorLogin() { return authorLogin; }
    public void setAuthorLogin(String authorLogin) { this.authorLogin = authorLogin; }

    public String getBaseBranch() { return baseBranch; }
    public void setBaseBranch(String baseBranch) { this.baseBranch = baseBranch; }

    public String getHeadBranch() { return headBranch; }
    public void setHeadBranch(String headBranch) { this.headBranch = headBranch; }

    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }

    public Integer getAdditions() { return additions; }
    public void setAdditions(Integer additions) { this.additions = additions; }

    public Integer getDeletions() { return deletions; }
    public void setDeletions(Integer deletions) { this.deletions = deletions; }

    public Integer getChangedFiles() { return changedFiles; }
    public void setChangedFiles(Integer changedFiles) { this.changedFiles = changedFiles; }

    public String getGithubCreatedAt() { return githubCreatedAt; }
    public void setGithubCreatedAt(String githubCreatedAt) { this.githubCreatedAt = githubCreatedAt; }

    public String getGithubUpdatedAt() { return githubUpdatedAt; }
    public void setGithubUpdatedAt(String githubUpdatedAt) { this.githubUpdatedAt = githubUpdatedAt; }
}
