package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class PrReviewQualityRecordResponse {

    private String id;
    private String projectId;
    private String reviewJobId;
    private String githubBindingId;
    private String repositoryFullName;
    private Long pullRequestNumber;
    private String strategyKey;
    private String modelProvider;
    private String modelName;
    private Integer findingsTotal;
    private Integer highRiskFindings;
    private Integer mediumRiskFindings;
    private Integer lowRiskFindings;
    private String reviewStatus;
    private String humanFeedbackStatus;
    private String adoptionStatus;
    private Integer usefulnessScore;
    private Integer falsePositiveScore;
    private String reviewComment;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getReviewJobId() {
        return reviewJobId;
    }

    public void setReviewJobId(String reviewJobId) {
        this.reviewJobId = reviewJobId;
    }

    public String getGithubBindingId() {
        return githubBindingId;
    }

    public void setGithubBindingId(String githubBindingId) {
        this.githubBindingId = githubBindingId;
    }

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public void setRepositoryFullName(String repositoryFullName) {
        this.repositoryFullName = repositoryFullName;
    }

    public Long getPullRequestNumber() {
        return pullRequestNumber;
    }

    public void setPullRequestNumber(Long pullRequestNumber) {
        this.pullRequestNumber = pullRequestNumber;
    }

    public String getStrategyKey() {
        return strategyKey;
    }

    public void setStrategyKey(String strategyKey) {
        this.strategyKey = strategyKey;
    }

    public String getModelProvider() {
        return modelProvider;
    }

    public void setModelProvider(String modelProvider) {
        this.modelProvider = modelProvider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getFindingsTotal() {
        return findingsTotal;
    }

    public void setFindingsTotal(Integer findingsTotal) {
        this.findingsTotal = findingsTotal;
    }

    public Integer getHighRiskFindings() {
        return highRiskFindings;
    }

    public void setHighRiskFindings(Integer highRiskFindings) {
        this.highRiskFindings = highRiskFindings;
    }

    public Integer getMediumRiskFindings() {
        return mediumRiskFindings;
    }

    public void setMediumRiskFindings(Integer mediumRiskFindings) {
        this.mediumRiskFindings = mediumRiskFindings;
    }

    public Integer getLowRiskFindings() {
        return lowRiskFindings;
    }

    public void setLowRiskFindings(Integer lowRiskFindings) {
        this.lowRiskFindings = lowRiskFindings;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getHumanFeedbackStatus() {
        return humanFeedbackStatus;
    }

    public void setHumanFeedbackStatus(String humanFeedbackStatus) {
        this.humanFeedbackStatus = humanFeedbackStatus;
    }

    public String getAdoptionStatus() {
        return adoptionStatus;
    }

    public void setAdoptionStatus(String adoptionStatus) {
        this.adoptionStatus = adoptionStatus;
    }

    public Integer getUsefulnessScore() {
        return usefulnessScore;
    }

    public void setUsefulnessScore(Integer usefulnessScore) {
        this.usefulnessScore = usefulnessScore;
    }

    public Integer getFalsePositiveScore() {
        return falsePositiveScore;
    }

    public void setFalsePositiveScore(Integer falsePositiveScore) {
        this.falsePositiveScore = falsePositiveScore;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
