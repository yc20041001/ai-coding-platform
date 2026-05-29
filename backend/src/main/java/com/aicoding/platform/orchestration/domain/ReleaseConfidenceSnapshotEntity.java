package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("release_confidence_snapshot")
public class ReleaseConfidenceSnapshotEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long projectId;

    private Long planId;

    private String releaseLabel;

    private BigDecimal confidenceScore;

    private String confidenceLevel;

    private Integer blockingIssueCount;

    private Integer warningIssueCount;

    private Integer openIncidentCount;

    private Integer activeAlertCount;

    private Integer failedVerificationCount;

    private Integer rollbackReady;

    private BigDecimal signoffCompletionRate;

    private String snapshotSummary;

    private LocalDateTime snapshotTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getReleaseLabel() { return releaseLabel; }
    public void setReleaseLabel(String releaseLabel) { this.releaseLabel = releaseLabel; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
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
    public String getSnapshotSummary() { return snapshotSummary; }
    public void setSnapshotSummary(String snapshotSummary) { this.snapshotSummary = snapshotSummary; }
    public LocalDateTime getSnapshotTime() { return snapshotTime; }
    public void setSnapshotTime(LocalDateTime snapshotTime) { this.snapshotTime = snapshotTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
