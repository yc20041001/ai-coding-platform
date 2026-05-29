package com.aicoding.platform.orchestration.dto;

import java.time.LocalDate;
import java.util.List;

public class ReleaseGuardrailDashboardResponse {

    private LocalDate snapshotDate;
    private Integer projectCount;
    private Integer passCount;
    private Integer warnCount;
    private Integer blockCount;
    private Integer criticalCount;
    private List<ReleaseGuardrailEvaluationResponse> topBlockedProjects;
    private List<ReleaseGuardrailEvaluationResponse> topWarningProjects;
    private Integer recommendationCount;

    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
    public Integer getProjectCount() { return projectCount; }
    public void setProjectCount(Integer projectCount) { this.projectCount = projectCount; }
    public Integer getPassCount() { return passCount; }
    public void setPassCount(Integer passCount) { this.passCount = passCount; }
    public Integer getWarnCount() { return warnCount; }
    public void setWarnCount(Integer warnCount) { this.warnCount = warnCount; }
    public Integer getBlockCount() { return blockCount; }
    public void setBlockCount(Integer blockCount) { this.blockCount = blockCount; }
    public Integer getCriticalCount() { return criticalCount; }
    public void setCriticalCount(Integer criticalCount) { this.criticalCount = criticalCount; }
    public List<ReleaseGuardrailEvaluationResponse> getTopBlockedProjects() { return topBlockedProjects; }
    public void setTopBlockedProjects(List<ReleaseGuardrailEvaluationResponse> topBlockedProjects) { this.topBlockedProjects = topBlockedProjects; }
    public List<ReleaseGuardrailEvaluationResponse> getTopWarningProjects() { return topWarningProjects; }
    public void setTopWarningProjects(List<ReleaseGuardrailEvaluationResponse> topWarningProjects) { this.topWarningProjects = topWarningProjects; }
    public Integer getRecommendationCount() { return recommendationCount; }
    public void setRecommendationCount(Integer recommendationCount) { this.recommendationCount = recommendationCount; }
}
