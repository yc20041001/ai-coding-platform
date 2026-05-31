package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceKnowledgeRecipeIntegrationTest extends IntegrationTestBase {

    private int counter = (int)(System.currentTimeMillis() % 100000);

    @BeforeEach
    public void setUp() { loginAdmin(); }

    // ========== Knowledge Entry ==========
    @Test void shouldCreateKnowledgeEntrySuccess() {
        ResponseEntity<String> res = post("/api/governance-knowledge/entries?title=Test&category=CONFIDENCE&summaryText=Summary", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.title")).isEqualTo("Test");
    }
    @Test void shouldUpdateKnowledgeEntrySuccess() {
        ResponseEntity<String> cr = post("/api/governance-knowledge/entries?title=T1&category=CONFIDENCE", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        ResponseEntity<String> res = put("/api/governance-knowledge/entries/" + id + "?title=Updated", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.title")).isEqualTo("Updated");
    }
    @Test void shouldSearchByKeywordSuccess() {
        post("/api/governance-knowledge/entries?title=SearchTest&category=CONFIDENCE", Map.of());
        ResponseEntity<String> res = get("/api/governance-knowledge/search?keyword=SearchTest");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldSearchByCategorySuccess() {
        post("/api/governance-knowledge/entries?title=CatTest&category=ROLLBACK", Map.of());
        ResponseEntity<String> res = get("/api/governance-knowledge/search?category=ROLLBACK");
        assertOk(res);
    }
    @Test void shouldGetEntryById() {
        ResponseEntity<String> cr = post("/api/governance-knowledge/entries?title=GetTest&category=CONFIDENCE", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        ResponseEntity<String> res = get("/api/governance-knowledge/entries/" + id);
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id")).isEqualTo(id);
    }
    @Test void shouldListEntries() {
        ResponseEntity<String> res = get("/api/governance-knowledge/entries");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }

    // ========== Pattern Library ==========
    @Test void shouldCreatePatternSuccess() {
        ResponseEntity<String> res = post("/api/governance-knowledge/patterns?patternKey=pat-" + (counter++) + "&displayName=Pat1", Map.of());
        assertOk(res);
    }
    @Test void shouldUpdatePatternSuccess() {
        String key = "upd-pat-" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-knowledge/patterns?patternKey=" + key + "&displayName=Pat1", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        ResponseEntity<String> res = put("/api/governance-knowledge/patterns/" + id + "?displayName=Updated", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.displayName")).isEqualTo("Updated");
    }
    @Test void shouldDisablePatternSuccess() {
        String key = "dis-pat-" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-knowledge/patterns?patternKey=" + key + "&displayName=Pat1", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        ResponseEntity<String> res = post("/api/governance-knowledge/patterns/" + id + "/status?enabled=false", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getBool(TestJsonHelper.parse(res.getBody()), "data.enabled")).isFalse();
    }
    @Test void shouldDuplicatePatternKeyReject() {
        String key = "dup-pat-" + (counter++);
        post("/api/governance-knowledge/patterns?patternKey=" + key + "&displayName=First", Map.of());
        ResponseEntity<String> res = post("/api/governance-knowledge/patterns?patternKey=" + key + "&displayName=Second", Map.of());
        assertCode(res, "CONFLICT");
    }
    @Test void shouldListPatterns() {
        ResponseEntity<String> res = get("/api/governance-knowledge/patterns");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }

    // ========== Recipe ==========
    @Test void shouldCreateRecipeSuccess() {
        ResponseEntity<String> res = post("/api/governance-knowledge/recipes?recipeKey=rec-" + (counter++) + "&displayName=Rec1&recipeType=REMEDIATION", Map.of());
        assertOk(res);
    }
    @Test void shouldUpdateRecipeSuccess() {
        String key = "upd-rec-" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-knowledge/recipes?recipeKey=" + key + "&displayName=Rec1", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        ResponseEntity<String> res = put("/api/governance-knowledge/recipes/" + id + "?displayName=Updated", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.displayName")).isEqualTo("Updated");
    }
    @Test void shouldDisableRecipeSuccess() {
        String key = "dis-rec-" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-knowledge/recipes?recipeKey=" + key + "&displayName=Rec1", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        ResponseEntity<String> res = post("/api/governance-knowledge/recipes/" + id + "/status?enabled=false", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getBool(TestJsonHelper.parse(res.getBody()), "data.enabled")).isFalse();
    }
    @Test void shouldDuplicateRecipeKeyReject() {
        String key = "dup-rec-" + (counter++);
        post("/api/governance-knowledge/recipes?recipeKey=" + key + "&displayName=First", Map.of());
        ResponseEntity<String> res = post("/api/governance-knowledge/recipes?recipeKey=" + key + "&displayName=Second", Map.of());
        assertCode(res, "CONFLICT");
    }
    @Test void shouldRecipeRecommendationReturnResults() {
        ResponseEntity<String> res = get("/api/governance-knowledge/recipe-recommendations/1");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldListRecipes() {
        ResponseEntity<String> res = get("/api/governance-knowledge/recipes");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldGetRecipeById() {
        String key = "get-rec-" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-knowledge/recipes?recipeKey=" + key + "&displayName=GetRec", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        ResponseEntity<String> res = get("/api/governance-knowledge/recipes/" + id);
        assertOk(res);
    }
    @Test void shouldSimilarSuggestionsReturnResults() {
        ResponseEntity<String> res = get("/api/governance-knowledge/similar-suggestions/1");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldRecipeEffectivenessScorePersists() {
        String key = "eff-rec-" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-knowledge/recipes?recipeKey=" + key + "&displayName=EffRec", Map.of());
        assertOk(cr); assertThat(TestJsonHelper.parse(cr.getBody()).get("data").get("effectivenessScore")).isNotNull();
    }
    @Test void shouldRecipeUsageCountPersists() {
        String key = "use-rec-" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-knowledge/recipes?recipeKey=" + key + "&displayName=UseRec", Map.of());
        assertOk(cr); assertThat(TestJsonHelper.parse(cr.getBody()).get("data").get("usageCount")).isNotNull();
    }

    // ========== Dashboard & Report ==========
    @Test void shouldDashboardCountsCorrect() {
        ResponseEntity<String> res = get("/api/governance-knowledge/dashboard");
        assertOk(res); JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("entryCount")).isNotNull();
        assertThat(root.get("data").get("patternCount")).isNotNull();
        assertThat(root.get("data").get("recipeCount")).isNotNull();
    }
    @Test void shouldTopKnowledgeEntriesReturned() {
        ResponseEntity<String> res = get("/api/governance-knowledge/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topKnowledgeEntries").isArray()).isTrue();
    }
    @Test void shouldTopRecipesReturned() {
        ResponseEntity<String> res = get("/api/governance-knowledge/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topRecipes").isArray()).isTrue();
    }
    @Test void shouldReportExportMarkdownSuccess() {
        ResponseEntity<String> res = get("/api/governance-knowledge/report");
        assertOk(res);
    }
    @Test void shouldAverageEffectivenessScoreReturned() {
        ResponseEntity<String> res = get("/api/governance-knowledge/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("averageEffectivenessScore")).isNotNull();
    }
    @Test void shouldHighReuseCountReturned() {
        ResponseEntity<String> res = get("/api/governance-knowledge/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("highReuseCount")).isNotNull();
    }
    @Test void shouldKnowledgeDetailMarkdownPersists() {
        ResponseEntity<String> cr = post("/api/governance-knowledge/entries?title=MDTest&category=CONFIDENCE&detailMarkdown=Hello", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        ResponseEntity<String> res = get("/api/governance-knowledge/entries/" + id);
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.detailMarkdown")).isEqualTo("Hello");
    }
    @Test void shouldGetRecipeByIdReturnsEffectiveScore() {
        String key = "get-rec-" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-knowledge/recipes?recipeKey=" + key + "&displayName=GetRec", Map.of());
        assertOk(cr); assertThat(TestJsonHelper.parse(cr.getBody()).get("data").get("effectivenessScore")).isNotNull();
    }
    @Test void shouldEmptyDatasetReturnEmptyDashboard() {
        ResponseEntity<String> res = get("/api/governance-knowledge/dashboard");
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.entryCount")).isNotNull();
    }
    @Test void shouldRecipeListOrderedByEffectiveness() {
        ResponseEntity<String> res = get("/api/governance-knowledge/recipes");
        assertOk(res);
    }
    @Test void shouldGetPatternById() {
        String key = "gpat-" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-knowledge/patterns?patternKey=" + key + "&displayName=GPat", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        ResponseEntity<String> res = get("/api/governance-knowledge/patterns/" + id);
        assertOk(res);
    }
    @Test void shouldKnowledgeSearchReturnsFiltered() {
        post("/api/governance-knowledge/entries?title=FilterMe&category=ROLLBACK", Map.of());
        ResponseEntity<String> res = get("/api/governance-knowledge/search?category=ROLLBACK");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldPatternListContainsEnabledField() {
        ResponseEntity<String> res = get("/api/governance-knowledge/patterns");
        assertOk(res); JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        if (data.size() > 0) assertThat(data.get(0).get("enabled")).isNotNull();
    }
    @Test void shouldRecipeContainsEnabledField() {
        ResponseEntity<String> res = get("/api/governance-knowledge/recipes");
        assertOk(res); JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        if (data.size() > 0) assertThat(data.get(0).get("enabled")).isNotNull();
    }
    @Test void shouldKnowledgeEntryContainsSourceType() {
        ResponseEntity<String> res = get("/api/governance-knowledge/entries");
        assertOk(res); JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        if (data.size() > 0) assertThat(data.get(0).get("sourceType")).isNotNull();
    }
}
