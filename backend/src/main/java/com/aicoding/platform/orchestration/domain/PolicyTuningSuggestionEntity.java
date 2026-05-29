package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("policy_tuning_suggestion")
public class PolicyTuningSuggestionEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private LocalDate snapshotDate; private String suggestionType; private String priority;
    private String targetScope; private String targetKey;
    private String currentValue; private String suggestedValue;
    private String expectedImpactText; private String rationaleText; private String evidenceJson;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;

    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public String getSuggestionType() { return suggestionType; } public void setSuggestionType(String v) { this.suggestionType = v; }
    public String getPriority() { return priority; } public void setPriority(String v) { this.priority = v; }
    public String getTargetScope() { return targetScope; } public void setTargetScope(String v) { this.targetScope = v; }
    public String getTargetKey() { return targetKey; } public void setTargetKey(String v) { this.targetKey = v; }
    public String getCurrentValue() { return currentValue; } public void setCurrentValue(String v) { this.currentValue = v; }
    public String getSuggestedValue() { return suggestedValue; } public void setSuggestedValue(String v) { this.suggestedValue = v; }
    public String getExpectedImpactText() { return expectedImpactText; } public void setExpectedImpactText(String v) { this.expectedImpactText = v; }
    public String getRationaleText() { return rationaleText; } public void setRationaleText(String v) { this.rationaleText = v; }
    public String getEvidenceJson() { return evidenceJson; } public void setEvidenceJson(String v) { this.evidenceJson = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
