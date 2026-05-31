package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("governance_recommendation_execution_plan")
public class GovernanceRecommendationExecutionPlanEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long recommendationId; private Long projectId; private String planStatus;
    private String templateKey; private Long ownerId; private String ownerName; private LocalDateTime dueAt;
    private String stepsJson; private BigDecimal completionRate; private String summaryText;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;

    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getRecommendationId() { return recommendationId; } public void setRecommendationId(Long v) { this.recommendationId = v; }
    public Long getProjectId() { return projectId; } public void setProjectId(Long v) { this.projectId = v; }
    public String getPlanStatus() { return planStatus; } public void setPlanStatus(String v) { this.planStatus = v; }
    public String getTemplateKey() { return templateKey; } public void setTemplateKey(String v) { this.templateKey = v; }
    public Long getOwnerId() { return ownerId; } public void setOwnerId(Long v) { this.ownerId = v; }
    public String getOwnerName() { return ownerName; } public void setOwnerName(String v) { this.ownerName = v; }
    public LocalDateTime getDueAt() { return dueAt; } public void setDueAt(LocalDateTime v) { this.dueAt = v; }
    public String getStepsJson() { return stepsJson; } public void setStepsJson(String v) { this.stepsJson = v; }
    public BigDecimal getCompletionRate() { return completionRate; } public void setCompletionRate(BigDecimal v) { this.completionRate = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
