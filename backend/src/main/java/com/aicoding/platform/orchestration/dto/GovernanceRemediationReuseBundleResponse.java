package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;

public class GovernanceRemediationReuseBundleResponse {
    private String id; private String bundleKey; private String title; private String category;
    private String guardrailKey; private String priority; private String effectivenessLevel;
    private Integer reuseCount; private BigDecimal successRate; private String actionSequenceJson;
    private String sourceSessionId; private String sourceOperatorId; private String sourceOperatorName;
    private Boolean enabled; private String summaryText;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getBundleKey() { return bundleKey; } public void setBundleKey(String v) { this.bundleKey = v; }
    public String getTitle() { return title; } public void setTitle(String v) { this.title = v; }
    public String getCategory() { return category; } public void setCategory(String v) { this.category = v; }
    public String getGuardrailKey() { return guardrailKey; } public void setGuardrailKey(String v) { this.guardrailKey = v; }
    public String getPriority() { return priority; } public void setPriority(String v) { this.priority = v; }
    public String getEffectivenessLevel() { return effectivenessLevel; } public void setEffectivenessLevel(String v) { this.effectivenessLevel = v; }
    public Integer getReuseCount() { return reuseCount; } public void setReuseCount(Integer v) { this.reuseCount = v; }
    public BigDecimal getSuccessRate() { return successRate; } public void setSuccessRate(BigDecimal v) { this.successRate = v; }
    public String getActionSequenceJson() { return actionSequenceJson; } public void setActionSequenceJson(String v) { this.actionSequenceJson = v; }
    public String getSourceSessionId() { return sourceSessionId; } public void setSourceSessionId(String v) { this.sourceSessionId = v; }
    public String getSourceOperatorId() { return sourceOperatorId; } public void setSourceOperatorId(String v) { this.sourceOperatorId = v; }
    public String getSourceOperatorName() { return sourceOperatorName; } public void setSourceOperatorName(String v) { this.sourceOperatorName = v; }
    public Boolean getEnabled() { return enabled; } public void setEnabled(Boolean v) { this.enabled = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
}
