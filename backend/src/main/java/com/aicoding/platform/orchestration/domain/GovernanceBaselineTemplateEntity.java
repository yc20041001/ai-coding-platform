package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("governance_baseline_template")
public class GovernanceBaselineTemplateEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String templateKey;
    private String displayName;
    private String templateScope;
    private Integer enabled;
    private String defaultSignoffRolesJson;
    private String defaultVerificationRulesJson;
    private String defaultRollbackRequirementsJson;
    private String defaultConfidencePolicyJson;
    private String notes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getTemplateScope() { return templateScope; }
    public void setTemplateScope(String templateScope) { this.templateScope = templateScope; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public String getDefaultSignoffRolesJson() { return defaultSignoffRolesJson; }
    public void setDefaultSignoffRolesJson(String defaultSignoffRolesJson) { this.defaultSignoffRolesJson = defaultSignoffRolesJson; }
    public String getDefaultVerificationRulesJson() { return defaultVerificationRulesJson; }
    public void setDefaultVerificationRulesJson(String defaultVerificationRulesJson) { this.defaultVerificationRulesJson = defaultVerificationRulesJson; }
    public String getDefaultRollbackRequirementsJson() { return defaultRollbackRequirementsJson; }
    public void setDefaultRollbackRequirementsJson(String defaultRollbackRequirementsJson) { this.defaultRollbackRequirementsJson = defaultRollbackRequirementsJson; }
    public String getDefaultConfidencePolicyJson() { return defaultConfidencePolicyJson; }
    public void setDefaultConfidencePolicyJson(String defaultConfidencePolicyJson) { this.defaultConfidencePolicyJson = defaultConfidencePolicyJson; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
