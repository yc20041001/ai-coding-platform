package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("governance_optimization_suggestion")
public class GovernanceOptimizationSuggestionEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private LocalDate snapshotDate; private String suggestionType; private String priority;
    private String targetType; private String targetKey; private String currentMetricValue;
    private String suggestedAction; private String expectedImpactText; private String rationaleText;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public String getSuggestionType() { return suggestionType; } public void setSuggestionType(String v) { this.suggestionType = v; }
    public String getPriority() { return priority; } public void setPriority(String v) { this.priority = v; }
    public String getTargetType() { return targetType; } public void setTargetType(String v) { this.targetType = v; }
    public String getTargetKey() { return targetKey; } public void setTargetKey(String v) { this.targetKey = v; }
    public String getCurrentMetricValue() { return currentMetricValue; } public void setCurrentMetricValue(String v) { this.currentMetricValue = v; }
    public String getSuggestedAction() { return suggestedAction; } public void setSuggestedAction(String v) { this.suggestedAction = v; }
    public String getExpectedImpactText() { return expectedImpactText; } public void setExpectedImpactText(String v) { this.expectedImpactText = v; }
    public String getRationaleText() { return rationaleText; } public void setRationaleText(String v) { this.rationaleText = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
