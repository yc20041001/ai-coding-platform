package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("governance_maturity_scorecard")
public class GovernanceMaturityScorecardEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private LocalDate snapshotDate; private Long projectId; private String projectName;
    private String maturityLevel; private BigDecimal totalScore;
    private BigDecimal draftAdoptionScore; private BigDecimal assistiveQualityScore;
    private BigDecimal packageQualityScore; private BigDecimal outcomeReviewScore;
    private BigDecimal operatorProductivityScore; private String summaryText;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public Long getProjectId() { return projectId; } public void setProjectId(Long v) { this.projectId = v; }
    public String getProjectName() { return projectName; } public void setProjectName(String v) { this.projectName = v; }
    public String getMaturityLevel() { return maturityLevel; } public void setMaturityLevel(String v) { this.maturityLevel = v; }
    public BigDecimal getTotalScore() { return totalScore; } public void setTotalScore(BigDecimal v) { this.totalScore = v; }
    public BigDecimal getDraftAdoptionScore() { return draftAdoptionScore; } public void setDraftAdoptionScore(BigDecimal v) { this.draftAdoptionScore = v; }
    public BigDecimal getAssistiveQualityScore() { return assistiveQualityScore; } public void setAssistiveQualityScore(BigDecimal v) { this.assistiveQualityScore = v; }
    public BigDecimal getPackageQualityScore() { return packageQualityScore; } public void setPackageQualityScore(BigDecimal v) { this.packageQualityScore = v; }
    public BigDecimal getOutcomeReviewScore() { return outcomeReviewScore; } public void setOutcomeReviewScore(BigDecimal v) { this.outcomeReviewScore = v; }
    public BigDecimal getOperatorProductivityScore() { return operatorProductivityScore; } public void setOperatorProductivityScore(BigDecimal v) { this.operatorProductivityScore = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
