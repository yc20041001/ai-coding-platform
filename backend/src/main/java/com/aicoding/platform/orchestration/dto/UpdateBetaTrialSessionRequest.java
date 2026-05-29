package com.aicoding.platform.orchestration.dto;

public class UpdateBetaTrialSessionRequest {

    private String sessionStatus;
    private String blockedAtStep;
    private String blockerSummary;
    private String completedPathJson;
    private Integer satisfactionScore;
    private String continueIntent;
    private String summary;
    private String startedAt;
    private String endedAt;

    public UpdateBetaTrialSessionRequest() {}

    public String getSessionStatus() { return sessionStatus; }
    public void setSessionStatus(String sessionStatus) { this.sessionStatus = sessionStatus; }

    public String getBlockedAtStep() { return blockedAtStep; }
    public void setBlockedAtStep(String blockedAtStep) { this.blockedAtStep = blockedAtStep; }

    public String getBlockerSummary() { return blockerSummary; }
    public void setBlockerSummary(String blockerSummary) { this.blockerSummary = blockerSummary; }

    public String getCompletedPathJson() { return completedPathJson; }
    public void setCompletedPathJson(String completedPathJson) { this.completedPathJson = completedPathJson; }

    public Integer getSatisfactionScore() { return satisfactionScore; }
    public void setSatisfactionScore(Integer satisfactionScore) { this.satisfactionScore = satisfactionScore; }

    public String getContinueIntent() { return continueIntent; }
    public void setContinueIntent(String continueIntent) { this.continueIntent = continueIntent; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }

    public String getEndedAt() { return endedAt; }
    public void setEndedAt(String endedAt) { this.endedAt = endedAt; }
}
