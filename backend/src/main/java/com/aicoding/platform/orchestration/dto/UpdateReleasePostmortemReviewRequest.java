package com.aicoding.platform.orchestration.dto;

public class UpdateReleasePostmortemReviewRequest {

    private String overallOutcome;
    private String summary;
    private String whatWentWell;
    private String whatWentWrong;
    private String customerImpact;
    private String followUpActions;
    private String reviewerId;

    public String getOverallOutcome() { return overallOutcome; }
    public void setOverallOutcome(String overallOutcome) { this.overallOutcome = overallOutcome; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getWhatWentWell() { return whatWentWell; }
    public void setWhatWentWell(String whatWentWell) { this.whatWentWell = whatWentWell; }
    public String getWhatWentWrong() { return whatWentWrong; }
    public void setWhatWentWrong(String whatWentWrong) { this.whatWentWrong = whatWentWrong; }
    public String getCustomerImpact() { return customerImpact; }
    public void setCustomerImpact(String customerImpact) { this.customerImpact = customerImpact; }
    public String getFollowUpActions() { return followUpActions; }
    public void setFollowUpActions(String followUpActions) { this.followUpActions = followUpActions; }
    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
}
