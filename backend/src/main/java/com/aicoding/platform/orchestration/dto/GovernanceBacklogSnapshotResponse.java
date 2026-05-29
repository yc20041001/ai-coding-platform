package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GovernanceBacklogSnapshotResponse {
    private String id; private LocalDate snapshotDate; private String projectId; private String projectName;
    private Integer openCount; private Integer inProgressCount; private Integer blockedCount;
    private Integer overdueCount; private Integer waiverActiveCount; private Integer incoming7dCount;
    private Integer completed7dCount; private BigDecimal backlogGrowthRate; private String backlogHealthLevel;
    private String summaryText;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public String getProjectId() { return projectId; } public void setProjectId(String v) { this.projectId = v; }
    public String getProjectName() { return projectName; } public void setProjectName(String v) { this.projectName = v; }
    public Integer getOpenCount() { return openCount; } public void setOpenCount(Integer v) { this.openCount = v; }
    public Integer getInProgressCount() { return inProgressCount; } public void setInProgressCount(Integer v) { this.inProgressCount = v; }
    public Integer getBlockedCount() { return blockedCount; } public void setBlockedCount(Integer v) { this.blockedCount = v; }
    public Integer getOverdueCount() { return overdueCount; } public void setOverdueCount(Integer v) { this.overdueCount = v; }
    public Integer getWaiverActiveCount() { return waiverActiveCount; } public void setWaiverActiveCount(Integer v) { this.waiverActiveCount = v; }
    public Integer getIncoming7dCount() { return incoming7dCount; } public void setIncoming7dCount(Integer v) { this.incoming7dCount = v; }
    public Integer getCompleted7dCount() { return completed7dCount; } public void setCompleted7dCount(Integer v) { this.completed7dCount = v; }
    public BigDecimal getBacklogGrowthRate() { return backlogGrowthRate; } public void setBacklogGrowthRate(BigDecimal v) { this.backlogGrowthRate = v; }
    public String getBacklogHealthLevel() { return backlogHealthLevel; } public void setBacklogHealthLevel(String v) { this.backlogHealthLevel = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
}
