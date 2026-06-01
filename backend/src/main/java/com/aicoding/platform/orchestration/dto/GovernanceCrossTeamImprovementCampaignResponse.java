package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class GovernanceCrossTeamImprovementCampaignResponse {
    private String id; private String campaignKey; private String campaignName; private String campaignStatus;
    private String targetProjectIdsJson; private String sourceProjectId; private String sourcePracticeType;
    private String improvementWindow; private String goalText; private String notesText;
    private LocalDateTime createTime; private LocalDateTime updateTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getCampaignKey() { return campaignKey; } public void setCampaignKey(String v) { this.campaignKey = v; }
    public String getCampaignName() { return campaignName; } public void setCampaignName(String v) { this.campaignName = v; }
    public String getCampaignStatus() { return campaignStatus; } public void setCampaignStatus(String v) { this.campaignStatus = v; }
    public String getTargetProjectIdsJson() { return targetProjectIdsJson; } public void setTargetProjectIdsJson(String v) { this.targetProjectIdsJson = v; }
    public String getSourceProjectId() { return sourceProjectId; } public void setSourceProjectId(String v) { this.sourceProjectId = v; }
    public String getSourcePracticeType() { return sourcePracticeType; } public void setSourcePracticeType(String v) { this.sourcePracticeType = v; }
    public String getImprovementWindow() { return improvementWindow; } public void setImprovementWindow(String v) { this.improvementWindow = v; }
    public String getGoalText() { return goalText; } public void setGoalText(String v) { this.goalText = v; }
    public String getNotesText() { return notesText; } public void setNotesText(String v) { this.notesText = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
