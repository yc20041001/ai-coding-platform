package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("governance_workspace_session")
public class GovernanceWorkspaceSessionEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long operatorId; private String operatorName; private String sessionStatus;
    private String focusMode; private Long selectedProjectId; private Long selectedRecommendationId;
    private Long selectedOwnerId; private String contextSummary;
    private LocalDateTime startedAt; private LocalDateTime endedAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getOperatorId() { return operatorId; } public void setOperatorId(Long v) { this.operatorId = v; }
    public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { this.operatorName = v; }
    public String getSessionStatus() { return sessionStatus; } public void setSessionStatus(String v) { this.sessionStatus = v; }
    public String getFocusMode() { return focusMode; } public void setFocusMode(String v) { this.focusMode = v; }
    public Long getSelectedProjectId() { return selectedProjectId; } public void setSelectedProjectId(Long v) { this.selectedProjectId = v; }
    public Long getSelectedRecommendationId() { return selectedRecommendationId; } public void setSelectedRecommendationId(Long v) { this.selectedRecommendationId = v; }
    public Long getSelectedOwnerId() { return selectedOwnerId; } public void setSelectedOwnerId(Long v) { this.selectedOwnerId = v; }
    public String getContextSummary() { return contextSummary; } public void setContextSummary(String v) { this.contextSummary = v; }
    public LocalDateTime getStartedAt() { return startedAt; } public void setStartedAt(LocalDateTime v) { this.startedAt = v; }
    public LocalDateTime getEndedAt() { return endedAt; } public void setEndedAt(LocalDateTime v) { this.endedAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
