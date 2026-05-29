package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class MultiProjectGovernanceSummaryResponse {

    private LocalDate snapshotDate;
    private Integer totalProjectCount;
    private Integer expandNowCount;
    private Integer expandWithGuardrailsCount;
    private Integer holdCount;
    private Integer blockCount;
    private BigDecimal averageConfidenceScore;
    private List<String> riskiestProjects;
    private String improvingProject;
    private String decliningProject;
    private String summaryMarkdown;

    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
    public Integer getTotalProjectCount() { return totalProjectCount; }
    public void setTotalProjectCount(Integer totalProjectCount) { this.totalProjectCount = totalProjectCount; }
    public Integer getExpandNowCount() { return expandNowCount; }
    public void setExpandNowCount(Integer expandNowCount) { this.expandNowCount = expandNowCount; }
    public Integer getExpandWithGuardrailsCount() { return expandWithGuardrailsCount; }
    public void setExpandWithGuardrailsCount(Integer expandWithGuardrailsCount) { this.expandWithGuardrailsCount = expandWithGuardrailsCount; }
    public Integer getHoldCount() { return holdCount; }
    public void setHoldCount(Integer holdCount) { this.holdCount = holdCount; }
    public Integer getBlockCount() { return blockCount; }
    public void setBlockCount(Integer blockCount) { this.blockCount = blockCount; }
    public BigDecimal getAverageConfidenceScore() { return averageConfidenceScore; }
    public void setAverageConfidenceScore(BigDecimal averageConfidenceScore) { this.averageConfidenceScore = averageConfidenceScore; }
    public List<String> getRiskiestProjects() { return riskiestProjects; }
    public void setRiskiestProjects(List<String> riskiestProjects) { this.riskiestProjects = riskiestProjects; }
    public String getImprovingProject() { return improvingProject; }
    public void setImprovingProject(String improvingProject) { this.improvingProject = improvingProject; }
    public String getDecliningProject() { return decliningProject; }
    public void setDecliningProject(String decliningProject) { this.decliningProject = decliningProject; }
    public String getSummaryMarkdown() { return summaryMarkdown; }
    public void setSummaryMarkdown(String summaryMarkdown) { this.summaryMarkdown = summaryMarkdown; }
}
