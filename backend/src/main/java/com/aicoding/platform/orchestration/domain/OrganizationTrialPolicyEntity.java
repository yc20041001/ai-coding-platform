package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("organization_trial_policy")
public class OrganizationTrialPolicyEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String policyKey;
    private String displayName;
    private String policyScope;
    private Integer enabled;
    private String thresholdJson;
    private String signoffPolicyJson;
    private String rollbackPolicyJson;
    private String verificationPolicyJson;
    private String recommendationPolicyJson;
    private String notes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPolicyKey() { return policyKey; }
    public void setPolicyKey(String policyKey) { this.policyKey = policyKey; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPolicyScope() { return policyScope; }
    public void setPolicyScope(String policyScope) { this.policyScope = policyScope; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public String getThresholdJson() { return thresholdJson; }
    public void setThresholdJson(String thresholdJson) { this.thresholdJson = thresholdJson; }
    public String getSignoffPolicyJson() { return signoffPolicyJson; }
    public void setSignoffPolicyJson(String signoffPolicyJson) { this.signoffPolicyJson = signoffPolicyJson; }
    public String getRollbackPolicyJson() { return rollbackPolicyJson; }
    public void setRollbackPolicyJson(String rollbackPolicyJson) { this.rollbackPolicyJson = rollbackPolicyJson; }
    public String getVerificationPolicyJson() { return verificationPolicyJson; }
    public void setVerificationPolicyJson(String verificationPolicyJson) { this.verificationPolicyJson = verificationPolicyJson; }
    public String getRecommendationPolicyJson() { return recommendationPolicyJson; }
    public void setRecommendationPolicyJson(String recommendationPolicyJson) { this.recommendationPolicyJson = recommendationPolicyJson; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
