package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("governance_remediation_reuse_bundle")
public class GovernanceRemediationReuseBundleEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private String bundleKey; private String title; private String category; private String guardrailKey;
    private String priority; private String effectivenessLevel; private Integer reuseCount;
    private BigDecimal successRate; private String actionSequenceJson;
    private Long sourceSessionId; private Long sourceOperatorId; private String sourceOperatorName;
    private Integer enabled; private String summaryText;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public String getBundleKey() { return bundleKey; } public void setBundleKey(String v) { this.bundleKey = v; }
    public String getTitle() { return title; } public void setTitle(String v) { this.title = v; }
    public String getCategory() { return category; } public void setCategory(String v) { this.category = v; }
    public String getGuardrailKey() { return guardrailKey; } public void setGuardrailKey(String v) { this.guardrailKey = v; }
    public String getPriority() { return priority; } public void setPriority(String v) { this.priority = v; }
    public String getEffectivenessLevel() { return effectivenessLevel; } public void setEffectivenessLevel(String v) { this.effectivenessLevel = v; }
    public Integer getReuseCount() { return reuseCount; } public void setReuseCount(Integer v) { this.reuseCount = v; }
    public BigDecimal getSuccessRate() { return successRate; } public void setSuccessRate(BigDecimal v) { this.successRate = v; }
    public String getActionSequenceJson() { return actionSequenceJson; } public void setActionSequenceJson(String v) { this.actionSequenceJson = v; }
    public Long getSourceSessionId() { return sourceSessionId; } public void setSourceSessionId(Long v) { this.sourceSessionId = v; }
    public Long getSourceOperatorId() { return sourceOperatorId; } public void setSourceOperatorId(Long v) { this.sourceOperatorId = v; }
    public String getSourceOperatorName() { return sourceOperatorName; } public void setSourceOperatorName(String v) { this.sourceOperatorName = v; }
    public Integer getEnabled() { return enabled; } public void setEnabled(Integer v) { this.enabled = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
