package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("governance_ownership_snapshot")
public class GovernanceOwnershipSnapshotEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private LocalDate snapshotDate;
    private Long ownerId;
    private String ownerName;
    private Integer totalAssignedCount;
    private Integer openCount;
    private Integer inProgressCount;
    private Integer overdueCount;
    @TableField("completed_7d_count")
    private Integer completed7dCount;
    private Integer activeWaiverCount;
    private BigDecimal ownerHealthScore;
    private String ownerHealthLevel;
    private String summaryText;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;

    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public Long getOwnerId() { return ownerId; } public void setOwnerId(Long v) { this.ownerId = v; }
    public String getOwnerName() { return ownerName; } public void setOwnerName(String v) { this.ownerName = v; }
    public Integer getTotalAssignedCount() { return totalAssignedCount; } public void setTotalAssignedCount(Integer v) { this.totalAssignedCount = v; }
    public Integer getOpenCount() { return openCount; } public void setOpenCount(Integer v) { this.openCount = v; }
    public Integer getInProgressCount() { return inProgressCount; } public void setInProgressCount(Integer v) { this.inProgressCount = v; }
    public Integer getOverdueCount() { return overdueCount; } public void setOverdueCount(Integer v) { this.overdueCount = v; }
    public Integer getCompleted7dCount() { return completed7dCount; } public void setCompleted7dCount(Integer v) { this.completed7dCount = v; }
    public Integer getActiveWaiverCount() { return activeWaiverCount; } public void setActiveWaiverCount(Integer v) { this.activeWaiverCount = v; }
    public BigDecimal getOwnerHealthScore() { return ownerHealthScore; } public void setOwnerHealthScore(BigDecimal v) { this.ownerHealthScore = v; }
    public String getOwnerHealthLevel() { return ownerHealthLevel; } public void setOwnerHealthLevel(String v) { this.ownerHealthLevel = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
