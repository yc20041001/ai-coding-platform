package com.aicoding.platform.repository.dto;

import jakarta.validation.constraints.NotBlank;

public class BindRepositoryRequest {

    @NotBlank(message = "provider 不能为空")
    private String provider;

    @NotBlank(message = "repoFullName 不能为空")
    private String repoFullName;

    @NotBlank(message = "repoUrl 不能为空")
    private String repoUrl;

    @NotBlank(message = "cloneUrl 不能为空")
    private String cloneUrl;

    private String defaultBranch;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getRepoFullName() { return repoFullName; }
    public void setRepoFullName(String repoFullName) { this.repoFullName = repoFullName; }

    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }

    public String getCloneUrl() { return cloneUrl; }
    public void setCloneUrl(String cloneUrl) { this.cloneUrl = cloneUrl; }

    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }
}
