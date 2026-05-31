package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("governance_recommendation_playbook_template")
public class GovernanceRecommendationPlaybookTemplateEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private String templateKey; private String displayName; private String recommendationCategory;
    private String guardrailKey; private String priority; private Integer enabled;
    private String templateStepsJson; private String successCriteriaJson; private String handoffNotes;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;

    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public String getTemplateKey() { return templateKey; } public void setTemplateKey(String v) { this.templateKey = v; }
    public String getDisplayName() { return displayName; } public void setDisplayName(String v) { this.displayName = v; }
    public String getRecommendationCategory() { return recommendationCategory; } public void setRecommendationCategory(String v) { this.recommendationCategory = v; }
    public String getGuardrailKey() { return guardrailKey; } public void setGuardrailKey(String v) { this.guardrailKey = v; }
    public String getPriority() { return priority; } public void setPriority(String v) { this.priority = v; }
    public Integer getEnabled() { return enabled; } public void setEnabled(Integer v) { this.enabled = v; }
    public String getTemplateStepsJson() { return templateStepsJson; } public void setTemplateStepsJson(String v) { this.templateStepsJson = v; }
    public String getSuccessCriteriaJson() { return successCriteriaJson; } public void setSuccessCriteriaJson(String v) { this.successCriteriaJson = v; }
    public String getHandoffNotes() { return handoffNotes; } public void setHandoffNotes(String v) { this.handoffNotes = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
