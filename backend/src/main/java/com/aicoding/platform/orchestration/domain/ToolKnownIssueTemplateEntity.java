package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("tool_known_issue_template")
public class ToolKnownIssueTemplateEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private String title;
    private String category;
    private String severity;
    private String rootCauseTemplate;
    private String impactTemplate;
    private String resolutionTemplate;
    private String preventionTemplate;
    private String tags;
    private Boolean enabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public ToolKnownIssueTemplateEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getRootCauseTemplate() { return rootCauseTemplate; }
    public void setRootCauseTemplate(String rootCauseTemplate) { this.rootCauseTemplate = rootCauseTemplate; }

    public String getImpactTemplate() { return impactTemplate; }
    public void setImpactTemplate(String impactTemplate) { this.impactTemplate = impactTemplate; }

    public String getResolutionTemplate() { return resolutionTemplate; }
    public void setResolutionTemplate(String resolutionTemplate) { this.resolutionTemplate = resolutionTemplate; }

    public String getPreventionTemplate() { return preventionTemplate; }
    public void setPreventionTemplate(String preventionTemplate) { this.preventionTemplate = preventionTemplate; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
