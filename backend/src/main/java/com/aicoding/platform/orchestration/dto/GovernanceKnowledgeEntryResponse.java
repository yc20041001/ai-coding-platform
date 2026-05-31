package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GovernanceKnowledgeEntryResponse {
    private String id; private String projectId; private String sourceType; private String sourceId;
    private String title; private String category; private String tagsJson; private String summaryText;
    private String detailMarkdown; private BigDecimal effectivenessScore; private Integer reuseCount;
    private LocalDateTime createTime; private LocalDateTime updateTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getProjectId() { return projectId; } public void setProjectId(String v) { this.projectId = v; }
    public String getSourceType() { return sourceType; } public void setSourceType(String v) { this.sourceType = v; }
    public String getSourceId() { return sourceId; } public void setSourceId(String v) { this.sourceId = v; }
    public String getTitle() { return title; } public void setTitle(String v) { this.title = v; }
    public String getCategory() { return category; } public void setCategory(String v) { this.category = v; }
    public String getTagsJson() { return tagsJson; } public void setTagsJson(String v) { this.tagsJson = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
    public String getDetailMarkdown() { return detailMarkdown; } public void setDetailMarkdown(String v) { this.detailMarkdown = v; }
    public BigDecimal getEffectivenessScore() { return effectivenessScore; } public void setEffectivenessScore(BigDecimal v) { this.effectivenessScore = v; }
    public Integer getReuseCount() { return reuseCount; } public void setReuseCount(Integer v) { this.reuseCount = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
