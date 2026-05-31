package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("governance_handoff_checklist")
public class GovernanceHandoffChecklistEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long recommendationId; private Long executionPlanId;
    private Long fromOwnerId; private String fromOwnerName;
    private Long toOwnerId; private String toOwnerName;
    private String checklistStatus; private String checklistItemsJson;
    private String handoffNote; private LocalDateTime handedOffAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;

    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getRecommendationId() { return recommendationId; } public void setRecommendationId(Long v) { this.recommendationId = v; }
    public Long getExecutionPlanId() { return executionPlanId; } public void setExecutionPlanId(Long v) { this.executionPlanId = v; }
    public Long getFromOwnerId() { return fromOwnerId; } public void setFromOwnerId(Long v) { this.fromOwnerId = v; }
    public String getFromOwnerName() { return fromOwnerName; } public void setFromOwnerName(String v) { this.fromOwnerName = v; }
    public Long getToOwnerId() { return toOwnerId; } public void setToOwnerId(Long v) { this.toOwnerId = v; }
    public String getToOwnerName() { return toOwnerName; } public void setToOwnerName(String v) { this.toOwnerName = v; }
    public String getChecklistStatus() { return checklistStatus; } public void setChecklistStatus(String v) { this.checklistStatus = v; }
    public String getChecklistItemsJson() { return checklistItemsJson; } public void setChecklistItemsJson(String v) { this.checklistItemsJson = v; }
    public String getHandoffNote() { return handoffNote; } public void setHandoffNote(String v) { this.handoffNote = v; }
    public LocalDateTime getHandedOffAt() { return handedOffAt; } public void setHandedOffAt(LocalDateTime v) { this.handedOffAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
