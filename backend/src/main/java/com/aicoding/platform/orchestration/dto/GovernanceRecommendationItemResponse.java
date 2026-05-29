package com.aicoding.platform.orchestration.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class GovernanceRecommendationItemResponse {

    private String id;
    private String projectId;
    private String projectName;
    private LocalDate sourceSnapshotDate;
    private String policyKey;
    private String guardrailKey;
    private String category;
    private String priority;
    private String workflowStatus;
    private String title;
    private String summary;
    private String ownerId;
    private String ownerName;
    private LocalDateTime dueAt;
    private LocalDateTime resolvedAt;
    private String resolutionNote;
    private String waiverStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public LocalDate getSourceSnapshotDate() { return sourceSnapshotDate; }
    public void setSourceSnapshotDate(LocalDate sourceSnapshotDate) { this.sourceSnapshotDate = sourceSnapshotDate; }
    public String getPolicyKey() { return policyKey; }
    public void setPolicyKey(String policyKey) { this.policyKey = policyKey; }
    public String getGuardrailKey() { return guardrailKey; }
    public void setGuardrailKey(String guardrailKey) { this.guardrailKey = guardrailKey; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getWorkflowStatus() { return workflowStatus; }
    public void setWorkflowStatus(String workflowStatus) { this.workflowStatus = workflowStatus; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
    public String getWaiverStatus() { return waiverStatus; }
    public void setWaiverStatus(String waiverStatus) { this.waiverStatus = waiverStatus; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
