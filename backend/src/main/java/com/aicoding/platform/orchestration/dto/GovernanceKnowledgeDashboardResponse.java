package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.util.List;

public class GovernanceKnowledgeDashboardResponse {
    private Integer entryCount; private Integer patternCount; private Integer recipeCount;
    private List<GovernanceKnowledgeEntryResponse> topKnowledgeEntries;
    private List<GovernanceRemediationRecipeResponse> topRecipes;
    private List<GovernancePatternLibraryItemResponse> topPatterns;
    private BigDecimal averageEffectivenessScore; private Integer highReuseCount;
    public Integer getEntryCount() { return entryCount; } public void setEntryCount(Integer v) { this.entryCount = v; }
    public Integer getPatternCount() { return patternCount; } public void setPatternCount(Integer v) { this.patternCount = v; }
    public Integer getRecipeCount() { return recipeCount; } public void setRecipeCount(Integer v) { this.recipeCount = v; }
    public List<GovernanceKnowledgeEntryResponse> getTopKnowledgeEntries() { return topKnowledgeEntries; }
    public void setTopKnowledgeEntries(List<GovernanceKnowledgeEntryResponse> v) { this.topKnowledgeEntries = v; }
    public List<GovernanceRemediationRecipeResponse> getTopRecipes() { return topRecipes; }
    public void setTopRecipes(List<GovernanceRemediationRecipeResponse> v) { this.topRecipes = v; }
    public List<GovernancePatternLibraryItemResponse> getTopPatterns() { return topPatterns; }
    public void setTopPatterns(List<GovernancePatternLibraryItemResponse> v) { this.topPatterns = v; }
    public BigDecimal getAverageEffectivenessScore() { return averageEffectivenessScore; }
    public void setAverageEffectivenessScore(BigDecimal v) { this.averageEffectivenessScore = v; }
    public Integer getHighReuseCount() { return highReuseCount; } public void setHighReuseCount(Integer v) { this.highReuseCount = v; }
}
