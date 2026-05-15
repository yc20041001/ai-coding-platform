package com.aicoding.platform.github.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("github_pull_request_cache")
public class GithubPullRequestCacheEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private Long repositoryId;
    private Long githubPrId;
    private Long githubRepoId;
    private Integer number;
    private String title;
    private String state;
    private String authorLogin;
    private String baseBranch;
    private String headBranch;
    private String htmlUrl;
    private String diffUrl;
    private String patchUrl;
    private Integer additions;
    private Integer deletions;
    private Integer changedFiles;
    private LocalDateTime githubCreatedAt;
    private LocalDateTime githubUpdatedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getRepositoryId() { return repositoryId; }
    public void setRepositoryId(Long repositoryId) { this.repositoryId = repositoryId; }

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

    public String getDiffUrl() { return diffUrl; }
    public void setDiffUrl(String diffUrl) { this.diffUrl = diffUrl; }

    public String getPatchUrl() { return patchUrl; }
    public void setPatchUrl(String patchUrl) { this.patchUrl = patchUrl; }

    public Integer getAdditions() { return additions; }
    public void setAdditions(Integer additions) { this.additions = additions; }

    public Integer getDeletions() { return deletions; }
    public void setDeletions(Integer deletions) { this.deletions = deletions; }

    public Integer getChangedFiles() { return changedFiles; }
    public void setChangedFiles(Integer changedFiles) { this.changedFiles = changedFiles; }

    public LocalDateTime getGithubCreatedAt() { return githubCreatedAt; }
    public void setGithubCreatedAt(LocalDateTime githubCreatedAt) { this.githubCreatedAt = githubCreatedAt; }

    public LocalDateTime getGithubUpdatedAt() { return githubUpdatedAt; }
    public void setGithubUpdatedAt(LocalDateTime githubUpdatedAt) { this.githubUpdatedAt = githubUpdatedAt; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
