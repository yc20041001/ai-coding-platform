package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("predictive_risk_signal")
public class PredictiveRiskSignalEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private LocalDate snapshotDate; private String targetType; private Long targetId;
    private String targetName; private String signalType; private String riskLevel;
    private BigDecimal riskScore; private BigDecimal probabilityScore;
    private Integer timeHorizonDays; private String summary; private String detail;
    private String evidenceJson;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public String getTargetType() { return targetType; } public void setTargetType(String v) { this.targetType = v; }
    public Long getTargetId() { return targetId; } public void setTargetId(Long v) { this.targetId = v; }
    public String getTargetName() { return targetName; } public void setTargetName(String v) { this.targetName = v; }
    public String getSignalType() { return signalType; } public void setSignalType(String v) { this.signalType = v; }
    public String getRiskLevel() { return riskLevel; } public void setRiskLevel(String v) { this.riskLevel = v; }
    public BigDecimal getRiskScore() { return riskScore; } public void setRiskScore(BigDecimal v) { this.riskScore = v; }
    public BigDecimal getProbabilityScore() { return probabilityScore; } public void setProbabilityScore(BigDecimal v) { this.probabilityScore = v; }
    public Integer getTimeHorizonDays() { return timeHorizonDays; } public void setTimeHorizonDays(Integer v) { this.timeHorizonDays = v; }
    public String getSummary() { return summary; } public void setSummary(String v) { this.summary = v; }
    public String getDetail() { return detail; } public void setDetail(String v) { this.detail = v; }
    public String getEvidenceJson() { return evidenceJson; } public void setEvidenceJson(String v) { this.evidenceJson = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
