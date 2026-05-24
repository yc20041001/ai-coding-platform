package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class IncidentKnowledgeSummaryResponse {

    private Long totalLinks;
    private Long documentCount;
    private Long templateCount;
    private Long rootCauseNoteCount;
    private LocalDateTime latestLinkTime;

    public IncidentKnowledgeSummaryResponse() {}

    public Long getTotalLinks() { return totalLinks; }
    public void setTotalLinks(Long totalLinks) { this.totalLinks = totalLinks; }

    public Long getDocumentCount() { return documentCount; }
    public void setDocumentCount(Long documentCount) { this.documentCount = documentCount; }

    public Long getTemplateCount() { return templateCount; }
    public void setTemplateCount(Long templateCount) { this.templateCount = templateCount; }

    public Long getRootCauseNoteCount() { return rootCauseNoteCount; }
    public void setRootCauseNoteCount(Long rootCauseNoteCount) { this.rootCauseNoteCount = rootCauseNoteCount; }

    public LocalDateTime getLatestLinkTime() { return latestLinkTime; }
    public void setLatestLinkTime(LocalDateTime latestLinkTime) { this.latestLinkTime = latestLinkTime; }
}
