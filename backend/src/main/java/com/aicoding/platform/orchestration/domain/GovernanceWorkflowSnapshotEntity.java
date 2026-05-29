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

@TableName("governance_workflow_snapshot")
public class GovernanceWorkflowSnapshotEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private LocalDate snapshotDate;
    private Integer totalRecommendationCount;
    private Integer openRecommendationCount;
    private Integer inProgressCount;
    private Integer completedCount;
    private Integer blockedCount;
    private Integer overdueCount;
    private Integer activeWaiverCount;
    private Integer expiredWaiverCount;
    private BigDecimal completionRate;
    private BigDecimal overdueRate;
    private String summaryText;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
    public Integer getTotalRecommendationCount() { return totalRecommendationCount; }
    public void setTotalRecommendationCount(Integer totalRecommendationCount) { this.totalRecommendationCount = totalRecommendationCount; }
    public Integer getOpenRecommendationCount() { return openRecommendationCount; }
    public void setOpenRecommendationCount(Integer openRecommendationCount) { this.openRecommendationCount = openRecommendationCount; }
    public Integer getInProgressCount() { return inProgressCount; }
    public void setInProgressCount(Integer inProgressCount) { this.inProgressCount = inProgressCount; }
    public Integer getCompletedCount() { return completedCount; }
    public void setCompletedCount(Integer completedCount) { this.completedCount = completedCount; }
    public Integer getBlockedCount() { return blockedCount; }
    public void setBlockedCount(Integer blockedCount) { this.blockedCount = blockedCount; }
    public Integer getOverdueCount() { return overdueCount; }
    public void setOverdueCount(Integer overdueCount) { this.overdueCount = overdueCount; }
    public Integer getActiveWaiverCount() { return activeWaiverCount; }
    public void setActiveWaiverCount(Integer activeWaiverCount) { this.activeWaiverCount = activeWaiverCount; }
    public Integer getExpiredWaiverCount() { return expiredWaiverCount; }
    public void setExpiredWaiverCount(Integer expiredWaiverCount) { this.expiredWaiverCount = expiredWaiverCount; }
    public BigDecimal getCompletionRate() { return completionRate; }
    public void setCompletionRate(BigDecimal completionRate) { this.completionRate = completionRate; }
    public BigDecimal getOverdueRate() { return overdueRate; }
    public void setOverdueRate(BigDecimal overdueRate) { this.overdueRate = overdueRate; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
