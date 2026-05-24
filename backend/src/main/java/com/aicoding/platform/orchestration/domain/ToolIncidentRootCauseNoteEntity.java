package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("tool_incident_root_cause_note")
public class ToolIncidentRootCauseNoteEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private Long incidentId;
    private String rootCause;
    private String impact;
    private String resolution;
    private String prevention;
    private String followUpActions;
    private String tags;
    private String confidence;
    private String status;
    private Long authorId;
    private Long lastEditorId;
    private LocalDateTime publishedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public ToolIncidentRootCauseNoteEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }

    public String getImpact() { return impact; }
    public void setImpact(String impact) { this.impact = impact; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public String getPrevention() { return prevention; }
    public void setPrevention(String prevention) { this.prevention = prevention; }

    public String getFollowUpActions() { return followUpActions; }
    public void setFollowUpActions(String followUpActions) { this.followUpActions = followUpActions; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public Long getLastEditorId() { return lastEditorId; }
    public void setLastEditorId(Long lastEditorId) { this.lastEditorId = lastEditorId; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
