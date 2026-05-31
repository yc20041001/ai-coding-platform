package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.*;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class GovernanceKnowledgeController {

    private final GovernanceKnowledgeBaseService knowledgeBaseService;
    private final GovernancePatternLibraryService patternLibraryService;
    private final GovernanceRemediationRecipeService remediationRecipeService;
    private final GovernanceKnowledgeBaseService knowledgeService;

    public GovernanceKnowledgeController(GovernanceKnowledgeBaseService knowledgeBaseService,
                                          GovernancePatternLibraryService patternLibraryService,
                                          GovernanceRemediationRecipeService remediationRecipeService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.patternLibraryService = patternLibraryService;
        this.remediationRecipeService = remediationRecipeService;
        this.knowledgeService = knowledgeBaseService;
    }

    // ========== Knowledge Entry ==========
    @PostMapping("/api/governance-knowledge/entries")
    public ApiResponse<GovernanceKnowledgeEntryResponse> createEntry(@RequestParam String title, @RequestParam String category,
                                                                       @RequestParam(required = false) String sourceType,
                                                                       @RequestParam(required = false) String summaryText,
                                                                       @RequestParam(required = false) String detailMarkdown,
                                                                       @RequestParam(required = false) String tagsJson) {
        return ApiResponse.ok(knowledgeBaseService.createEntry(title, category, sourceType, summaryText, detailMarkdown, tagsJson));
    }
    @GetMapping("/api/governance-knowledge/entries")
    public ApiResponse<List<GovernanceKnowledgeEntryResponse>> listEntries() {
        return ApiResponse.ok(knowledgeBaseService.listEntries());
    }
    @GetMapping("/api/governance-knowledge/entries/{entryId}")
    public ApiResponse<GovernanceKnowledgeEntryResponse> getEntry(@PathVariable String entryId) {
        return ApiResponse.ok(knowledgeBaseService.getEntry(entryId));
    }
    @PutMapping("/api/governance-knowledge/entries/{entryId}")
    public ApiResponse<GovernanceKnowledgeEntryResponse> updateEntry(@PathVariable String entryId,
                                                                      @RequestParam(required = false) String title,
                                                                      @RequestParam(required = false) String summaryText,
                                                                      @RequestParam(required = false) String detailMarkdown,
                                                                      @RequestParam(required = false) String tagsJson) {
        return ApiResponse.ok(knowledgeBaseService.updateEntry(entryId, title, summaryText, detailMarkdown, tagsJson));
    }
    @GetMapping("/api/governance-knowledge/search")
    public ApiResponse<List<GovernanceKnowledgeEntryResponse>> searchEntries(@RequestParam(required = false) String keyword,
                                                                               @RequestParam(required = false) String category) {
        return ApiResponse.ok(knowledgeBaseService.search(keyword, category));
    }

    // ========== Pattern Library ==========
    @PostMapping("/api/governance-knowledge/patterns")
    public ApiResponse<GovernancePatternLibraryItemResponse> createPattern(@RequestParam String patternKey,
                                                                            @RequestParam String displayName,
                                                                            @RequestParam(required = false) String recommendationCategory,
                                                                            @RequestParam(required = false) String guardrailKey,
                                                                            @RequestParam(required = false) String priority,
                                                                            @RequestParam(required = false) String patternJson) {
        return ApiResponse.ok(patternLibraryService.createPattern(patternKey, displayName, recommendationCategory, guardrailKey, priority, patternJson));
    }
    @GetMapping("/api/governance-knowledge/patterns")
    public ApiResponse<List<GovernancePatternLibraryItemResponse>> listPatterns() {
        return ApiResponse.ok(patternLibraryService.listPatterns());
    }
    @GetMapping("/api/governance-knowledge/patterns/{patternId}")
    public ApiResponse<GovernancePatternLibraryItemResponse> getPattern(@PathVariable String patternId) {
        return ApiResponse.ok(patternLibraryService.getPattern(patternId));
    }
    @PutMapping("/api/governance-knowledge/patterns/{patternId}")
    public ApiResponse<GovernancePatternLibraryItemResponse> updatePattern(@PathVariable String patternId,
                                                                            @RequestParam(required = false) String displayName,
                                                                            @RequestParam(required = false) String patternJson,
                                                                            @RequestParam(required = false) String notes) {
        return ApiResponse.ok(patternLibraryService.updatePattern(patternId, displayName, patternJson, notes));
    }
    @PostMapping("/api/governance-knowledge/patterns/{patternId}/status")
    public ApiResponse<GovernancePatternLibraryItemResponse> updatePatternStatus(@PathVariable String patternId,
                                                                                  @RequestParam Boolean enabled) {
        return ApiResponse.ok(patternLibraryService.updatePatternStatus(patternId, enabled));
    }

    // ========== Recipe ==========
    @PostMapping("/api/governance-knowledge/recipes")
    public ApiResponse<GovernanceRemediationRecipeResponse> createRecipe(@RequestParam String recipeKey,
                                                                          @RequestParam String displayName,
                                                                          @RequestParam(required = false) String recipeType,
                                                                          @RequestParam(required = false) String recommendationCategory,
                                                                          @RequestParam(required = false) String guardrailKey,
                                                                          @RequestParam(required = false) String stepsJson) {
        return ApiResponse.ok(remediationRecipeService.createRecipe(recipeKey, displayName, recipeType, recommendationCategory, guardrailKey, stepsJson));
    }
    @GetMapping("/api/governance-knowledge/recipes")
    public ApiResponse<List<GovernanceRemediationRecipeResponse>> listRecipes() {
        return ApiResponse.ok(remediationRecipeService.listRecipes());
    }
    @GetMapping("/api/governance-knowledge/recipes/{recipeId}")
    public ApiResponse<GovernanceRemediationRecipeResponse> getRecipe(@PathVariable String recipeId) {
        return ApiResponse.ok(remediationRecipeService.getRecipe(recipeId));
    }
    @PutMapping("/api/governance-knowledge/recipes/{recipeId}")
    public ApiResponse<GovernanceRemediationRecipeResponse> updateRecipe(@PathVariable String recipeId,
                                                                          @RequestParam(required = false) String displayName,
                                                                          @RequestParam(required = false) String stepsJson) {
        return ApiResponse.ok(remediationRecipeService.updateRecipe(recipeId, displayName, stepsJson));
    }
    @PostMapping("/api/governance-knowledge/recipes/{recipeId}/status")
    public ApiResponse<GovernanceRemediationRecipeResponse> updateRecipeStatus(@PathVariable String recipeId,
                                                                                @RequestParam Boolean enabled) {
        return ApiResponse.ok(remediationRecipeService.updateRecipeStatus(recipeId, enabled));
    }
    @GetMapping("/api/governance-knowledge/recipe-recommendations/{recommendationId}")
    public ApiResponse<List<GovernanceRemediationRecipeResponse>> getRecipeRecommendations(@PathVariable String recommendationId) {
        return ApiResponse.ok(remediationRecipeService.getRecipeRecommendations(recommendationId));
    }
    @GetMapping("/api/governance-knowledge/similar-suggestions/{recommendationId}")
    public ApiResponse<List<GovernanceRemediationRecipeResponse>> getSimilarSuggestions(@PathVariable String recommendationId) {
        return ApiResponse.ok(remediationRecipeService.getRecipeRecommendations(recommendationId));
    }

    // ========== Dashboard & Report ==========
    @GetMapping("/api/governance-knowledge/dashboard")
    public ApiResponse<GovernanceKnowledgeDashboardResponse> getDashboard() {
        GovernanceKnowledgeDashboardResponse resp = new GovernanceKnowledgeDashboardResponse();
        resp.setEntryCount(knowledgeBaseService.listEntries().size());
        resp.setPatternCount(patternLibraryService.listPatterns().size());
        resp.setRecipeCount(remediationRecipeService.listRecipes().size());
        resp.setTopKnowledgeEntries(knowledgeBaseService.getTopEntries());
        resp.setTopRecipes(remediationRecipeService.getTopRecipes());
        resp.setTopPatterns(patternLibraryService.listPatterns().stream().limit(5).collect(java.util.stream.Collectors.toList()));
        double avgScore = resp.getTopRecipes().stream()
                .mapToDouble(r -> r.getEffectivenessScore() != null ? r.getEffectivenessScore().doubleValue() : 0)
                .average().orElse(0);
        resp.setAverageEffectivenessScore(java.math.BigDecimal.valueOf(avgScore));
        resp.setHighReuseCount((int) resp.getTopRecipes().stream().filter(r -> r.getUsageCount() != null && r.getUsageCount() >= 3).count());
        return ApiResponse.ok(resp);
    }

    @GetMapping("/api/governance-knowledge/report")
    public ApiResponse<String> getReport() {
        var entries = knowledgeBaseService.listEntries();
        var patterns = patternLibraryService.listPatterns();
        var recipes = remediationRecipeService.listRecipes();
        StringBuilder md = new StringBuilder();
        md.append("# Governance Knowledge Summary\n\n");
        md.append("- Knowledge Entries: ").append(entries.size()).append("\n");
        md.append("- Patterns: ").append(patterns.size()).append("\n");
        md.append("- Recipes: ").append(recipes.size()).append("\n\n");
        md.append("## Top Recipes\n\n");
        for (var r : recipes.stream().limit(5).collect(java.util.stream.Collectors.toList())) {
            md.append("- **").append(r.getDisplayName()).append("** (score: ").append(r.getEffectivenessScore()).append(", uses: ").append(r.getUsageCount()).append(")\n");
        }
        return ApiResponse.ok(md.toString());
    }
}
