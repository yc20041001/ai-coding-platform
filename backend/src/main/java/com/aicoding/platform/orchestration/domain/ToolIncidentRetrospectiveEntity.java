package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("tool_incident_retrospective")
public class ToolIncidentRetrospectiveEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private Long incidentId;
    private Long rootCauseNoteId;
    private String title;
    private String summary;
    private String whatHappened;
    private String impactSummary;
    private String responseSummary;
    private String lessonsLearned;
    private String preventionPlan;
    private String actionItems;
    private Long ownerId;
    private LocalDateTime dueAt;
    private String regressionRisk;
    private Boolean repeatedIncident;
    private String status;
    private LocalDateTime publishedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public ToolIncidentRetrospectiveEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }

    public Long getRootCauseNoteId() { return rootCauseNoteId; }
    public void setRootCauseNoteId(Long rootCauseNoteId) { this.rootCauseNoteId = rootCauseNoteId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getWhatHappened() { return whatHappened; }
    public void setWhatHappened(String whatHappened) { this.whatHappened = whatHappened; }

    public String getImpactSummary() { return impactSummary; }
    public void setImpactSummary(String impactSummary) { this.impactSummary = impactSummary; }

    public String getResponseSummary() { return responseSummary; }
    public void setResponseSummary(String responseSummary) { this.responseSummary = responseSummary; }

    public String getLessonsLearned() { return lessonsLearned; }
    public void setLessonsLearned(String lessonsLearned) { this.lessonsLearned = lessonsLearned; }

    public String getPreventionPlan() { return preventionPlan; }
    public void setPreventionPlan(String preventionPlan) { this.preventionPlan = preventionPlan; }

    public String getActionItems() { return actionItems; }
    public void setActionItems(String actionItems) { this.actionItems = actionItems; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }

    public String getRegressionRisk() { return regressionRisk; }
    public void setRegressionRisk(String regressionRisk) { this.regressionRisk = regressionRisk; }

    public Boolean getRepeatedIncident() { return repeatedIncident; }
    public void setRepeatedIncident(Boolean repeatedIncident) { this.repeatedIncident = repeatedIncident; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
