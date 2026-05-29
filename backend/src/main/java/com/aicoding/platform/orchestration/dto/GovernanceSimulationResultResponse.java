package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GovernanceSimulationResultResponse {
    private String id; private String scenarioId; private String resultStatus;
    private Integer impactedOwnerCount; private Integer impactedProjectCount;
    private BigDecimal projectedBacklogDelta; private BigDecimal projectedOverdueDelta;
    private BigDecimal projectedRiskDelta; private BigDecimal projectedCapacityDelta;
    private String summaryText; private String detailJson; private String reportMarkdown;
    private LocalDateTime calculatedAt; private LocalDateTime createTime; private LocalDateTime updateTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getScenarioId() { return scenarioId; } public void setScenarioId(String v) { this.scenarioId = v; }
    public String getResultStatus() { return resultStatus; } public void setResultStatus(String v) { this.resultStatus = v; }
    public Integer getImpactedOwnerCount() { return impactedOwnerCount; } public void setImpactedOwnerCount(Integer v) { this.impactedOwnerCount = v; }
    public Integer getImpactedProjectCount() { return impactedProjectCount; } public void setImpactedProjectCount(Integer v) { this.impactedProjectCount = v; }
    public BigDecimal getProjectedBacklogDelta() { return projectedBacklogDelta; } public void setProjectedBacklogDelta(BigDecimal v) { this.projectedBacklogDelta = v; }
    public BigDecimal getProjectedOverdueDelta() { return projectedOverdueDelta; } public void setProjectedOverdueDelta(BigDecimal v) { this.projectedOverdueDelta = v; }
    public BigDecimal getProjectedRiskDelta() { return projectedRiskDelta; } public void setProjectedRiskDelta(BigDecimal v) { this.projectedRiskDelta = v; }
    public BigDecimal getProjectedCapacityDelta() { return projectedCapacityDelta; } public void setProjectedCapacityDelta(BigDecimal v) { this.projectedCapacityDelta = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
    public String getDetailJson() { return detailJson; } public void setDetailJson(String v) { this.detailJson = v; }
    public String getReportMarkdown() { return reportMarkdown; } public void setReportMarkdown(String v) { this.reportMarkdown = v; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; } public void setCalculatedAt(LocalDateTime v) { this.calculatedAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
