package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GovernanceBestPracticeAlignmentItemResponse {
    private String id; private LocalDate snapshotDate; private String projectId; private String projectName;
    private String practiceType; private String alignmentLevel; private BigDecimal currentScore;
    private BigDecimal targetScore; private BigDecimal gap; private String suggestionText;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public String getProjectId() { return projectId; } public void setProjectId(String v) { this.projectId = v; }
    public String getProjectName() { return projectName; } public void setProjectName(String v) { this.projectName = v; }
    public String getPracticeType() { return practiceType; } public void setPracticeType(String v) { this.practiceType = v; }
    public String getAlignmentLevel() { return alignmentLevel; } public void setAlignmentLevel(String v) { this.alignmentLevel = v; }
    public BigDecimal getCurrentScore() { return currentScore; } public void setCurrentScore(BigDecimal v) { this.currentScore = v; }
    public BigDecimal getTargetScore() { return targetScore; } public void setTargetScore(BigDecimal v) { this.targetScore = v; }
    public BigDecimal getGap() { return gap; } public void setGap(BigDecimal v) { this.gap = v; }
    public String getSuggestionText() { return suggestionText; } public void setSuggestionText(String v) { this.suggestionText = v; }
}
