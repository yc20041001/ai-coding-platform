package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("release_portfolio_snapshot")
public class ReleasePortfolioSnapshotEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private LocalDate snapshotDate;
    private Long projectId;
    private String projectName;
    private String latestReleaseLabel;
    private BigDecimal confidenceScore;
    private String confidenceLevel;
    private String rolloutStatus;
    private String decisionStatus;
    private Integer blockingIssueCount;
    private Integer warningIssueCount;
    private Integer openIncidentCount;
    private Integer activeAlertCount;
    private Integer failedVerificationCount;
    private Integer rollbackReady;
    private BigDecimal signoffCompletionRate;
    private Integer portfolioRank;
    private String expansionRecommendation;
    private String summaryText;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getLatestReleaseLabel() { return latestReleaseLabel; }
    public void setLatestReleaseLabel(String latestReleaseLabel) { this.latestReleaseLabel = latestReleaseLabel; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    public String getRolloutStatus() { return rolloutStatus; }
    public void setRolloutStatus(String rolloutStatus) { this.rolloutStatus = rolloutStatus; }
    public String getDecisionStatus() { return decisionStatus; }
    public void setDecisionStatus(String decisionStatus) { this.decisionStatus = decisionStatus; }
    public Integer getBlockingIssueCount() { return blockingIssueCount; }
    public void setBlockingIssueCount(Integer blockingIssueCount) { this.blockingIssueCount = blockingIssueCount; }
    public Integer getWarningIssueCount() { return warningIssueCount; }
    public void setWarningIssueCount(Integer warningIssueCount) { this.warningIssueCount = warningIssueCount; }
    public Integer getOpenIncidentCount() { return openIncidentCount; }
    public void setOpenIncidentCount(Integer openIncidentCount) { this.openIncidentCount = openIncidentCount; }
    public Integer getActiveAlertCount() { return activeAlertCount; }
    public void setActiveAlertCount(Integer activeAlertCount) { this.activeAlertCount = activeAlertCount; }
    public Integer getFailedVerificationCount() { return failedVerificationCount; }
    public void setFailedVerificationCount(Integer failedVerificationCount) { this.failedVerificationCount = failedVerificationCount; }
    public Integer getRollbackReady() { return rollbackReady; }
    public void setRollbackReady(Integer rollbackReady) { this.rollbackReady = rollbackReady; }
    public BigDecimal getSignoffCompletionRate() { return signoffCompletionRate; }
    public void setSignoffCompletionRate(BigDecimal signoffCompletionRate) { this.signoffCompletionRate = signoffCompletionRate; }
    public Integer getPortfolioRank() { return portfolioRank; }
    public void setPortfolioRank(Integer portfolioRank) { this.portfolioRank = portfolioRank; }
    public String getExpansionRecommendation() { return expansionRecommendation; }
    public void setExpansionRecommendation(String expansionRecommendation) { this.expansionRecommendation = expansionRecommendation; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
