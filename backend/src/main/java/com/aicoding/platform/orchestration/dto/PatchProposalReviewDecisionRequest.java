package com.aicoding.platform.orchestration.dto;

import java.util.Map;

public class PatchProposalReviewDecisionRequest {

    private String decision;
    private String comment;
    private Boolean safetyConfirmed;
    private Map<String, Object> checklist;

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Boolean getSafetyConfirmed() { return safetyConfirmed; }
    public void setSafetyConfirmed(Boolean safetyConfirmed) { this.safetyConfirmed = safetyConfirmed; }

    public Map<String, Object> getChecklist() { return checklist; }
    public void setChecklist(Map<String, Object> checklist) { this.checklist = checklist; }
}
