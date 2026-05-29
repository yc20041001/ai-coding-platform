package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("pr_review_quality_record")
public class PrReviewQualityRecordEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private Long reviewJobId;
    private Long githubBindingId;
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
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getReviewJobId() {
        return reviewJobId;
    }

    public void setReviewJobId(Long reviewJobId) {
        this.reviewJobId = reviewJobId;
    }

    public Long getGithubBindingId() {
        return githubBindingId;
    }

    public void setGithubBindingId(Long githubBindingId) {
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

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
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
