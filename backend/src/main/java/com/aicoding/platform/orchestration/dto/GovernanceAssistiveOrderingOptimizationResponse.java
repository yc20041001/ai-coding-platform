package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;

public class GovernanceAssistiveOrderingOptimizationResponse {
    private String id; private String actionType; private BigDecimal avgUsefulnessRating;
    private BigDecimal avgActionOrder; private Integer usefulnessCount; private Integer notUsefulCount;
    private String optimizationLevel; private Integer suggestedNewOrder; private String rationaleText;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getActionType() { return actionType; } public void setActionType(String v) { this.actionType = v; }
    public BigDecimal getAvgUsefulnessRating() { return avgUsefulnessRating; } public void setAvgUsefulnessRating(BigDecimal v) { this.avgUsefulnessRating = v; }
    public BigDecimal getAvgActionOrder() { return avgActionOrder; } public void setAvgActionOrder(BigDecimal v) { this.avgActionOrder = v; }
    public Integer getUsefulnessCount() { return usefulnessCount; } public void setUsefulnessCount(Integer v) { this.usefulnessCount = v; }
    public Integer getNotUsefulCount() { return notUsefulCount; } public void setNotUsefulCount(Integer v) { this.notUsefulCount = v; }
    public String getOptimizationLevel() { return optimizationLevel; } public void setOptimizationLevel(String v) { this.optimizationLevel = v; }
    public Integer getSuggestedNewOrder() { return suggestedNewOrder; } public void setSuggestedNewOrder(Integer v) { this.suggestedNewOrder = v; }
    public String getRationaleText() { return rationaleText; } public void setRationaleText(String v) { this.rationaleText = v; }
}
