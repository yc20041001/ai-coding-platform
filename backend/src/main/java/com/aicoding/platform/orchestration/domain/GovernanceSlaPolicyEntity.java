package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("governance_sla_policy")
public class GovernanceSlaPolicyEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String policyKey;
    private String displayName;
    private String priority;
    private String category;
    private Integer slaHours;
    private Integer warningHours;
    private Integer enabled;
    private String notes;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getPolicyKey() { return policyKey; } public void setPolicyKey(String v) { this.policyKey = v; }
    public String getDisplayName() { return displayName; } public void setDisplayName(String v) { this.displayName = v; }
    public String getPriority() { return priority; } public void setPriority(String v) { this.priority = v; }
    public String getCategory() { return category; } public void setCategory(String v) { this.category = v; }
    public Integer getSlaHours() { return slaHours; } public void setSlaHours(Integer v) { this.slaHours = v; }
    public Integer getWarningHours() { return warningHours; } public void setWarningHours(Integer v) { this.warningHours = v; }
    public Integer getEnabled() { return enabled; } public void setEnabled(Integer v) { this.enabled = v; }
    public String getNotes() { return notes; } public void setNotes(String v) { this.notes = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
