package com.aicoding.platform.orchestration.dto;

import java.util.List;

public class SimilarIncidentRegressionCheckResponse {

    private Boolean repeatedIncident;
    private String regressionRisk;
    private Double highestScore;
    private Integer similarCount;
    private List<SimilarIncidentResponse> similarIncidents;

    public SimilarIncidentRegressionCheckResponse() {}

    public Boolean getRepeatedIncident() { return repeatedIncident; }
    public void setRepeatedIncident(Boolean repeatedIncident) { this.repeatedIncident = repeatedIncident; }

    public String getRegressionRisk() { return regressionRisk; }
    public void setRegressionRisk(String regressionRisk) { this.regressionRisk = regressionRisk; }

    public Double getHighestScore() { return highestScore; }
    public void setHighestScore(Double highestScore) { this.highestScore = highestScore; }

    public Integer getSimilarCount() { return similarCount; }
    public void setSimilarCount(Integer similarCount) { this.similarCount = similarCount; }

    public List<SimilarIncidentResponse> getSimilarIncidents() { return similarIncidents; }
    public void setSimilarIncidents(List<SimilarIncidentResponse> similarIncidents) { this.similarIncidents = similarIncidents; }
}
