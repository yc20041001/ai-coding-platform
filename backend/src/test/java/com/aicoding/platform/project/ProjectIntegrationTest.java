package com.aicoding.platform.project;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectIntegrationTest extends IntegrationTestBase {

    @Test
    void shouldCreateProjectAndAutoAssignOwner() {
        String uniqueName = "IT-Project-" + System.currentTimeMillis();
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", uniqueName,
                "description", "Integration test project",
                "techStack", List.of("Java", "Spring Boot")
        ));

        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        String projectId = TestJsonHelper.getString(root, "data.id");
        assertThat(projectId).isNotEmpty();
        assertThat(TestJsonHelper.getString(root, "data.name")).isEqualTo(uniqueName);
        assertThat(TestJsonHelper.getString(root, "data.status")).isEqualTo("ACTIVE");

        // Verify project appears in list
        ResponseEntity<String> listRes = get("/api/projects?page=1&pageSize=20");
        assertOk(listRes);
        JsonNode listRoot = TestJsonHelper.parse(listRes.getBody());
        String recordsJson = listRoot.get("data").get("records").toString();
        assertThat(recordsJson).contains(projectId);
    }

    @Test
    void shouldGetProjectDetail() {
        String uniqueName = "IT-Detail-" + System.currentTimeMillis();
        ResponseEntity<String> createRes = post("/api/projects", Map.of(
                "name", uniqueName,
                "description", "Detail test",
                "techStack", List.of("Vue", "TypeScript")
        ));
        JsonNode createRoot = TestJsonHelper.parse(createRes.getBody());
        String projectId = TestJsonHelper.getString(createRoot, "data.id");

        ResponseEntity<String> detailRes = get("/api/projects/" + projectId);
        assertOk(detailRes);
        JsonNode root = TestJsonHelper.parse(detailRes.getBody());
        assertThat(TestJsonHelper.getString(root, "data.id")).isEqualTo(projectId);
        assertThat(TestJsonHelper.getString(root, "data.name")).isEqualTo(uniqueName);
    }

    @Test
    void shouldGetProjectOverview() {
        String uniqueName = "IT-Overview-" + System.currentTimeMillis();
        ResponseEntity<String> createRes = post("/api/projects", Map.of(
                "name", uniqueName,
                "description", "Overview test",
                "techStack", List.of("Python")
        ));
        JsonNode createRoot = TestJsonHelper.parse(createRes.getBody());
        String projectId = TestJsonHelper.getString(createRoot, "data.id");

        ResponseEntity<String> overviewRes = get("/api/projects/" + projectId + "/overview");
        assertOk(overviewRes);
        JsonNode root = TestJsonHelper.parse(overviewRes.getBody());
        assertThat(TestJsonHelper.getLong(root, "data.taskCount")).isGreaterThanOrEqualTo(0);
        assertThat(TestJsonHelper.getLong(root, "data.memberCount")).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldListProjectMembers() {
        String uniqueName = "IT-Member-" + System.currentTimeMillis();
        ResponseEntity<String> createRes = post("/api/projects", Map.of(
                "name", uniqueName,
                "description", "Member test",
                "techStack", List.of("Go")
        ));
        JsonNode createRoot = TestJsonHelper.parse(createRes.getBody());
        String projectId = TestJsonHelper.getString(createRoot, "data.id");

        ResponseEntity<String> membersRes = get("/api/projects/" + projectId + "/members?page=1&pageSize=20");
        assertOk(membersRes);
        JsonNode root = TestJsonHelper.parse(membersRes.getBody());
        String recordsJson = root.get("data").get("records").toString();
        assertThat(recordsJson).contains("admin");
    }

    @Test
    void shouldRejectWithoutToken() {
        ResponseEntity<String> res = getNoAuth("/api/projects");
        assertCode(res, "UNAUTHORIZED");
    }
}
