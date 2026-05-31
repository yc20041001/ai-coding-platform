package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GovernanceMaturityScorecardResponse {
    private String id; private LocalDate snapshotDate; private String projectId; private String projectName;
    private String maturityLevel; private BigDecimal totalScore; private BigDecimal draftAdoptionScore;
    private BigDecimal assistiveQualityScore; private BigDecimal packageQualityScore;
    private BigDecimal outcomeReviewScore; private BigDecimal operatorProductivityScore; private String summaryText;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public String getProjectId() { return projectId; } public void setProjectId(String v) { this.projectId = v; }
    public String getProjectName() { return projectName; } public void setProjectName(String v) { this.projectName = v; }
    public String getMaturityLevel() { return maturityLevel; } public void setMaturityLevel(String v) { this.maturityLevel = v; }
    public BigDecimal getTotalScore() { return totalScore; } public void setTotalScore(BigDecimal v) { this.totalScore = v; }
    public BigDecimal getDraftAdoptionScore() { return draftAdoptionScore; } public void setDraftAdoptionScore(BigDecimal v) { this.draftAdoptionScore = v; }
    public BigDecimal getAssistiveQualityScore() { return assistiveQualityScore; } public void setAssistiveQualityScore(BigDecimal v) { this.assistiveQualityScore = v; }
    public BigDecimal getPackageQualityScore() { return packageQualityScore; } public void setPackageQualityScore(BigDecimal v) { this.packageQualityScore = v; }
    public BigDecimal getOutcomeReviewScore() { return outcomeReviewScore; } public void setOutcomeReviewScore(BigDecimal v) { this.outcomeReviewScore = v; }
    public BigDecimal getOperatorProductivityScore() { return operatorProductivityScore; } public void setOperatorProductivityScore(BigDecimal v) { this.operatorProductivityScore = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
}
