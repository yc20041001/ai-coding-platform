package com.aicoding.platform.orchestration.dto;

import java.time.LocalDate;
import java.util.List;

public class GovernanceEscalationDashboardResponse {
    private LocalDate snapshotDate; private Integer openEscalationCount; private Integer highEscalationCount;
    private Integer criticalEscalationCount; private Integer waiverExpiringSoonCount; private Integer waiverExpiredCount;
    private Integer ownerMissingCount; private List<GovernanceEscalationEventResponse> topEscalations;
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public Integer getOpenEscalationCount() { return openEscalationCount; } public void setOpenEscalationCount(Integer v) { this.openEscalationCount = v; }
    public Integer getHighEscalationCount() { return highEscalationCount; } public void setHighEscalationCount(Integer v) { this.highEscalationCount = v; }
    public Integer getCriticalEscalationCount() { return criticalEscalationCount; } public void setCriticalEscalationCount(Integer v) { this.criticalEscalationCount = v; }
    public Integer getWaiverExpiringSoonCount() { return waiverExpiringSoonCount; } public void setWaiverExpiringSoonCount(Integer v) { this.waiverExpiringSoonCount = v; }
    public Integer getWaiverExpiredCount() { return waiverExpiredCount; } public void setWaiverExpiredCount(Integer v) { this.waiverExpiredCount = v; }
    public Integer getOwnerMissingCount() { return ownerMissingCount; } public void setOwnerMissingCount(Integer v) { this.ownerMissingCount = v; }
    public List<GovernanceEscalationEventResponse> getTopEscalations() { return topEscalations; } public void setTopEscalations(List<GovernanceEscalationEventResponse> v) { this.topEscalations = v; }
}
