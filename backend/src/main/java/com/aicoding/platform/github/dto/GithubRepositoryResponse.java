package com.aicoding.platform.github.dto;

public class GithubRepositoryResponse {
    private String id;
    private Long githubRepoId;
    private String owner;
    private String repoName;
    private String fullName;
    private boolean privateRepo;
    private String defaultBranch;
    private String htmlUrl;
    private String description;
    private String language;
    private String githubUpdatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getGithubRepoId() { return githubRepoId; }
    public void setGithubRepoId(Long githubRepoId) { this.githubRepoId = githubRepoId; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getRepoName() { return repoName; }
    public void setRepoName(String repoName) { this.repoName = repoName; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public boolean isPrivateRepo() { return privateRepo; }
    public void setPrivateRepo(boolean privateRepo) { this.privateRepo = privateRepo; }

    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }

    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getGithubUpdatedAt() { return githubUpdatedAt; }
    public void setGithubUpdatedAt(String githubUpdatedAt) { this.githubUpdatedAt = githubUpdatedAt; }
}
