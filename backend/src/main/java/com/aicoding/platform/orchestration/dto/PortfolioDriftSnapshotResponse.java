package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PortfolioDriftSnapshotResponse {

    private String id;
    private LocalDate snapshotDate;
    private String projectId;
    private String projectName;
    private BigDecimal driftScore;
    private String driftLevel;
    private String baselineTemplateKey;
    private BigDecimal confidenceDelta;
    private BigDecimal signoffDelta;
    private BigDecimal verificationDelta;
    private Integer rollbackReadinessChanged;
    private String summaryText;
    private String detailJson;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public BigDecimal getDriftScore() { return driftScore; }
    public void setDriftScore(BigDecimal driftScore) { this.driftScore = driftScore; }
    public String getDriftLevel() { return driftLevel; }
    public void setDriftLevel(String driftLevel) { this.driftLevel = driftLevel; }
    public String getBaselineTemplateKey() { return baselineTemplateKey; }
    public void setBaselineTemplateKey(String baselineTemplateKey) { this.baselineTemplateKey = baselineTemplateKey; }
    public BigDecimal getConfidenceDelta() { return confidenceDelta; }
    public void setConfidenceDelta(BigDecimal confidenceDelta) { this.confidenceDelta = confidenceDelta; }
    public BigDecimal getSignoffDelta() { return signoffDelta; }
    public void setSignoffDelta(BigDecimal signoffDelta) { this.signoffDelta = signoffDelta; }
    public BigDecimal getVerificationDelta() { return verificationDelta; }
    public void setVerificationDelta(BigDecimal verificationDelta) { this.verificationDelta = verificationDelta; }
    public Integer getRollbackReadinessChanged() { return rollbackReadinessChanged; }
    public void setRollbackReadinessChanged(Integer rollbackReadinessChanged) { this.rollbackReadinessChanged = rollbackReadinessChanged; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public String getDetailJson() { return detailJson; }
    public void setDetailJson(String detailJson) { this.detailJson = detailJson; }
}
