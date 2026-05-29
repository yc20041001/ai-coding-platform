package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PredictiveRiskSignalResponse {
    private String id; private LocalDate snapshotDate; private String targetType;
    private String targetId; private String targetName; private String signalType;
    private String riskLevel; private BigDecimal riskScore; private BigDecimal probabilityScore;
    private Integer timeHorizonDays; private String summary; private String detail;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public String getTargetType() { return targetType; } public void setTargetType(String v) { this.targetType = v; }
    public String getTargetId() { return targetId; } public void setTargetId(String v) { this.targetId = v; }
    public String getTargetName() { return targetName; } public void setTargetName(String v) { this.targetName = v; }
    public String getSignalType() { return signalType; } public void setSignalType(String v) { this.signalType = v; }
    public String getRiskLevel() { return riskLevel; } public void setRiskLevel(String v) { this.riskLevel = v; }
    public BigDecimal getRiskScore() { return riskScore; } public void setRiskScore(BigDecimal v) { this.riskScore = v; }
    public BigDecimal getProbabilityScore() { return probabilityScore; } public void setProbabilityScore(BigDecimal v) { this.probabilityScore = v; }
    public Integer getTimeHorizonDays() { return timeHorizonDays; } public void setTimeHorizonDays(Integer v) { this.timeHorizonDays = v; }
    public String getSummary() { return summary; } public void setSummary(String v) { this.summary = v; }
    public String getDetail() { return detail; } public void setDetail(String v) { this.detail = v; }
}
