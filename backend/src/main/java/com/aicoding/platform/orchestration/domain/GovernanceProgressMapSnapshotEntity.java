package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("governance_progress_map_snapshot")
public class GovernanceProgressMapSnapshotEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private LocalDate snapshotDate; private Long projectId; private String projectName;
    private String metricKey; private BigDecimal baselineScore; private BigDecimal currentScore;
    private BigDecimal targetScore; private BigDecimal progressPercentage;
    private String signalLevel; private String summaryText;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public Long getProjectId() { return projectId; } public void setProjectId(Long v) { this.projectId = v; }
    public String getProjectName() { return projectName; } public void setProjectName(String v) { this.projectName = v; }
    public String getMetricKey() { return metricKey; } public void setMetricKey(String v) { this.metricKey = v; }
    public BigDecimal getBaselineScore() { return baselineScore; } public void setBaselineScore(BigDecimal v) { this.baselineScore = v; }
    public BigDecimal getCurrentScore() { return currentScore; } public void setCurrentScore(BigDecimal v) { this.currentScore = v; }
    public BigDecimal getTargetScore() { return targetScore; } public void setTargetScore(BigDecimal v) { this.targetScore = v; }
    public BigDecimal getProgressPercentage() { return progressPercentage; } public void setProgressPercentage(BigDecimal v) { this.progressPercentage = v; }
    public String getSignalLevel() { return signalLevel; } public void setSignalLevel(String v) { this.signalLevel = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
