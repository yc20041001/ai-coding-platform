package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("governance_pattern_library_item")
public class GovernancePatternLibraryItemEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private String patternKey; private String displayName; private String recommendationCategory;
    private String guardrailKey; private String priority; private String patternJson; private String notes;
    private Integer enabled;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public String getPatternKey() { return patternKey; } public void setPatternKey(String v) { this.patternKey = v; }
    public String getDisplayName() { return displayName; } public void setDisplayName(String v) { this.displayName = v; }
    public String getRecommendationCategory() { return recommendationCategory; } public void setRecommendationCategory(String v) { this.recommendationCategory = v; }
    public String getGuardrailKey() { return guardrailKey; } public void setGuardrailKey(String v) { this.guardrailKey = v; }
    public String getPriority() { return priority; } public void setPriority(String v) { this.priority = v; }
    public String getPatternJson() { return patternJson; } public void setPatternJson(String v) { this.patternJson = v; }
    public String getNotes() { return notes; } public void setNotes(String v) { this.notes = v; }
    public Integer getEnabled() { return enabled; } public void setEnabled(Integer v) { this.enabled = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
