package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("governance_knowledge_entry")
public class GovernanceKnowledgeEntryEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long projectId; private String sourceType; private Long sourceId; private String title;
    private String category; private String tagsJson; private String summaryText; private String detailMarkdown;
    private BigDecimal effectivenessScore; private Integer reuseCount;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getProjectId() { return projectId; } public void setProjectId(Long v) { this.projectId = v; }
    public String getSourceType() { return sourceType; } public void setSourceType(String v) { this.sourceType = v; }
    public Long getSourceId() { return sourceId; } public void setSourceId(Long v) { this.sourceId = v; }
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
