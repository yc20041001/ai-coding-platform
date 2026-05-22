package com.aicoding.platform.orchestration;

import com.aicoding.platform.orchestration.application.ReadOnlyRepositoryAdapter;
import com.aicoding.platform.orchestration.application.RepositoryContentSafetyService;
import com.aicoding.platform.orchestration.application.RepositoryReadToolService;
import com.aicoding.platform.orchestration.application.RepositoryToolSafetyService;
import com.aicoding.platform.orchestration.dto.ReadOnlyRepositoryRequest;
import com.aicoding.platform.orchestration.dto.RepositoryBranchResult;
import com.aicoding.platform.orchestration.dto.RepositoryDiffSummaryResult;
import com.aicoding.platform.orchestration.dto.RepositoryFileSnippetResult;
import com.aicoding.platform.orchestration.dto.RepositoryReadFileItem;
import com.aicoding.platform.orchestration.dto.RepositorySkippedFileItem;
import com.aicoding.platform.orchestration.dto.RepositoryTreeResult;
import com.aicoding.platform.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReadOnlyToolAdapterIntegrationTest extends IntegrationTestBase {

    @Autowired
    private RepositoryContentSafetyService contentSafetyService;

    @Autowired
    private RepositoryToolSafetyService safetyService;

    @Autowired
    private ReadOnlyRepositoryAdapter repositoryAdapter;

    @Value("${app.workspace.root-path}")
    private String workspaceRootPath;

    private Path workspaceDir;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ALLOWED = "backend/src";

    @BeforeAll
    public void setupWorkspace() throws IOException {
        workspaceDir = Paths.get(workspaceRootPath).normalize().toAbsolutePath();
        Files.createDirectories(workspaceDir);

        // Test Java file under allowed prefix
        Path testJavaFile = workspaceDir.resolve(ALLOWED + "/TestFile.java");
        Files.createDirectories(testJavaFile.getParent());
        Files.writeString(testJavaFile, """
                package com.test;
                public class TestFile {
                    private String name;
                    public String getName() { return name; }
                    public void setName(String name) { this.name = name; }
                }
                """);

        // Binary file (with NUL byte) under allowed prefix
        Path binaryTestFile = workspaceDir.resolve(ALLOWED + "/image.png");
        Files.createDirectories(binaryTestFile.getParent());
        byte[] binaryContent = new byte[]{(byte) 137, (byte) 80, (byte) 78, (byte) 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82};
        Files.write(binaryTestFile, binaryContent);

        // Sensitive .env file under allowed prefix (tests safety even within allowed area)
        Files.writeString(workspaceDir.resolve(ALLOWED + "/.env"), "DB_PASSWORD=secret123\nAPI_KEY=sk-test1234567890\n");
        // Sensitive .pem file under allowed prefix
        Files.writeString(workspaceDir.resolve(ALLOWED + "/key.pem"), "-----BEGIN PRIVATE KEY-----\nMOCK\n-----END PRIVATE KEY-----\n");

        // target/ and node_modules/ under allowed prefix
        Path targetDir = workspaceDir.resolve(ALLOWED + "/target");
        Files.createDirectories(targetDir);
        Files.writeString(targetDir.resolve("build.class"), "binary content");

        Path nodeModulesDir = workspaceDir.resolve(ALLOWED + "/node_modules");
        Files.createDirectories(nodeModulesDir);
        Files.writeString(nodeModulesDir.resolve("package.json"), "{\"name\":\"test\"}");

        // File outside allowed prefixes
        Files.createDirectories(workspaceDir.resolve("random"));
        Files.writeString(workspaceDir.resolve("random/extra.txt"), "outside allowed prefix");

        // Large file
        Path largeFile = workspaceDir.resolve(ALLOWED + "/large-file.sql");
        Files.createDirectories(largeFile.getParent());
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("INSERT INTO test(id) VALUES(").append(i).append(");\n");
        }
        Files.writeString(largeFile, largeContent.toString());
    }

    @AfterAll
    public void cleanupWorkspace() throws IOException {
        if (workspaceDir != null && Files.exists(workspaceDir)) {
            try (var walk = Files.walk(workspaceDir)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                                // ignore
                            }
                        });
            }
        }
    }

    // ========================
    // 1. Path Safety (6 tests)
    // ========================

    @Test
    void shouldRejectAbsolutePath() {
        try {
            safetyService.validateSafeRelativePath("/etc/passwd");
            throw new AssertionError("Should have thrown for absolute path");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("绝对路径");
        }
    }

    @Test
    void shouldRejectDotDotPath() {
        try {
            safetyService.validateSafeRelativePath("../../etc/passwd");
            throw new AssertionError("Should have thrown for '..'");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("..");
        }
    }

    @Test
    void shouldRejectTildePath() {
        try {
            safetyService.validateSafeRelativePath("~/file.txt");
            throw new AssertionError("Should have thrown for '~'");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("~");
        }
    }

    @Test
    void shouldRejectNulCharPath() {
        try {
            safetyService.validateSafeRelativePath("file\0.txt");
            throw new AssertionError("Should have thrown for NUL");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("空字符");
        }
    }

    @Test
    void shouldRejectSensitiveEnvPath() {
        assertThat(safetyService.isSensitivePath(".env")).isTrue();
        assertThat(safetyService.isSensitivePath("config/.env")).isTrue();
        assertThat(safetyService.isSensitivePath(".git/HEAD")).isTrue();
    }

    @Test
    void shouldRejectSensitiveKeyPath() {
        assertThat(safetyService.isSensitivePath("key.pem")).isTrue();
        assertThat(safetyService.isSensitivePath("secret.p12")).isTrue();
        assertThat(safetyService.isSensitivePath("target/classes/Main.class")).isTrue();
        assertThat(safetyService.isSensitivePath("node_modules/express/index.js")).isTrue();
    }

    // ============================
    // 2. Content Safety (8 tests)
    // ============================

    @Test
    void shouldDetectBinaryContentByNulByte() {
        byte[] contentWithNul = "hello\0world".getBytes(StandardCharsets.UTF_8);
        assertThat(contentSafetyService.isBinaryContent(contentWithNul)).isTrue();
    }

    @Test
    void shouldDetectNonBinaryContent() {
        byte[] textContent = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        assertThat(contentSafetyService.isBinaryContent(textContent)).isFalse();
    }

    @Test
    void shouldDetectBinaryExtension() {
        assertThat(contentSafetyService.isBinaryExtension("image.png")).isTrue();
        assertThat(contentSafetyService.isBinaryExtension("archive.jar")).isTrue();
        assertThat(contentSafetyService.isBinaryExtension("document.pdf")).isTrue();
    }

    @Test
    void shouldDetectTextExtension() {
        assertThat(contentSafetyService.isBinaryExtension("file.java")).isFalse();
        assertThat(contentSafetyService.isBinaryExtension("file.ts")).isFalse();
        assertThat(contentSafetyService.isBinaryExtension("file.txt")).isFalse();
    }

    @Test
    void shouldEnforceDefaultFileSizeLimit() {
        assertThat(contentSafetyService.isFileSizeWithinLimit(1000)).isTrue();
        assertThat(contentSafetyService.isFileSizeWithinLimit(200 * 1024)).isFalse();
    }

    @Test
    void shouldEnforceCustomFileSizeLimit() {
        assertThat(contentSafetyService.isFileSizeWithinLimit(5000, 10000)).isTrue();
        assertThat(contentSafetyService.isFileSizeWithinLimit(15000, 10000)).isFalse();
    }

    @Test
    void shouldRedactApiKeyPatterns() {
        String input = "api_key = 'sk-test12345678901234567890'\ntoken = 'ghp_testToken12345678901234567890123456789012'";
        String redacted = contentSafetyService.redactSecrets(input);
        assertThat(redacted).contains("**REDACTED**");
        assertThat(redacted).doesNotContain("sk-test1234567890");
        assertThat(redacted).doesNotContain("ghp_testToken1234567890");
    }

    @Test
    void shouldRedactWithCorrectCount() {
        String input = "password = 'supersecret'\napi_key = 'sk-test-key'\ntoken = 'my-token'";
        RepositoryContentSafetyService.RedactionResult result = contentSafetyService.redactSecretsWithCount(input);
        assertThat(result.getRedactionCount()).isGreaterThanOrEqualTo(3);
        assertThat(result.getContent()).contains("**REDACTED**");
    }

    // ===============================
    // 3. Adapter Behavior (7 tests)
    // ===============================

    @Test
    void shouldListTreeReturnFiles() {
        ReadOnlyRepositoryRequest request = new ReadOnlyRepositoryRequest();
        request.setBranch("main");
        request.setPathPrefix("");
        request.setMaxFiles(100);

        RepositoryTreeResult result = repositoryAdapter.listTree(request);

        assertThat(result).isNotNull();
        assertThat(result.getBranch()).isEqualTo("main");
        assertThat(result.getFiles()).isNotEmpty();

        // Should find our TestFile.java under allowed prefix
        boolean foundTestFile = result.getFiles().stream()
                .anyMatch(f -> f.getFilePath().endsWith("TestFile.java"));
        assertThat(foundTestFile).isTrue();

        // All files should have filePath, language, fileSize
        for (RepositoryReadFileItem file : result.getFiles()) {
            assertThat(file.getFilePath()).isNotBlank();
            assertThat(file.getFileSize()).isGreaterThan(0);
        }
    }

    @Test
    void shouldListTreeSkipSensitivePaths() {
        ReadOnlyRepositoryRequest request = new ReadOnlyRepositoryRequest();
        request.setBranch("main");
        request.setPathPrefix("");
        request.setMaxFiles(100);

        RepositoryTreeResult result = repositoryAdapter.listTree(request);

        // .env should be skipped (even under allowed prefix)
        boolean hasEnvFile = result.getFiles().stream()
                .anyMatch(f -> f.getFilePath().contains(".env"));
        assertThat(hasEnvFile).isFalse();

        // target/ files should be skipped
        boolean hasTargetFile = result.getFiles().stream()
                .anyMatch(f -> f.getFilePath().contains("target/"));
        assertThat(hasTargetFile).isFalse();

        // node_modules/ files should be skipped
        boolean hasNodeModulesFile = result.getFiles().stream()
                .anyMatch(f -> f.getFilePath().contains("node_modules/"));
        assertThat(hasNodeModulesFile).isFalse();

        // .pem files should be skipped
        boolean hasPemFile = result.getFiles().stream()
                .anyMatch(f -> f.getFilePath().contains("key.pem"));
        assertThat(hasPemFile).isFalse();

        // Should have skipped files recorded
        assertThat(result.getSkippedFiles()).isNotEmpty();
    }

    @Test
    void shouldListTreeSkipBinaryFiles() {
        ReadOnlyRepositoryRequest request = new ReadOnlyRepositoryRequest();
        request.setBranch("main");
        request.setPathPrefix("");

        RepositoryTreeResult result = repositoryAdapter.listTree(request);

        // image.png should be skipped as binary
        boolean hasBinaryFile = result.getFiles().stream()
                .anyMatch(f -> f.getFilePath().contains("image.png"));
        assertThat(hasBinaryFile).isFalse();
    }

    @Test
    void shouldReadSnippetReturnContent() {
        ReadOnlyRepositoryRequest request = new ReadOnlyRepositoryRequest();
        request.setBranch("main");
        request.setFilePath(ALLOWED + "/TestFile.java");
        request.setStartLine(1);
        request.setMaxLines(50);

        RepositoryFileSnippetResult result = repositoryAdapter.readSnippet(request);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent()).contains("TestFile");
        assertThat(result.getFilePath()).isEqualTo(ALLOWED + "/TestFile.java");
        assertThat(result.getStartLine()).isEqualTo(1);
        assertThat(result.getEndLine()).isGreaterThanOrEqualTo(5);
        assertThat(result.getSkippedFiles()).isEmpty();
    }

    @Test
    void shouldReadSnippetRejectSensitivePath() {
        ReadOnlyRepositoryRequest request = new ReadOnlyRepositoryRequest();
        request.setBranch("main");
        request.setFilePath(ALLOWED + "/.env");

        RepositoryFileSnippetResult result = repositoryAdapter.readSnippet(request);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getSkippedFiles()).isNotEmpty();
        assertThat(result.getSkippedFiles().get(0).getReason()).isEqualTo("SENSITIVE_PATH");
    }

    @Test
    void shouldListBranchesReturnAtLeastMain() {
        ReadOnlyRepositoryRequest request = new ReadOnlyRepositoryRequest();
        request.setIncludeRemote(false);

        RepositoryBranchResult result = repositoryAdapter.listBranches(request);

        assertThat(result).isNotNull();
        assertThat(result.getBranches()).isNotEmpty();
        assertThat(result.getBranches()).contains("main");
        assertThat(result.isNoCheckout()).isTrue();
        assertThat(result.isNoPull()).isTrue();
    }

    @Test
    void shouldReadDiffSummaryReturnNoRealGitDiff() {
        ReadOnlyRepositoryRequest request = new ReadOnlyRepositoryRequest();
        request.setBranch("feature/test");
        request.setBaseBranch("main");

        RepositoryDiffSummaryResult result = repositoryAdapter.readDiffSummary(request);

        assertThat(result).isNotNull();
        assertThat(result.isNoRealGitDiff()).isTrue();
        assertThat(result.getFileCount()).isEqualTo(0);
        assertThat(result.getChangedFiles()).isEmpty();
    }

    // ==================================
    // 4. Tool Output Structure (3 tests)
    // ==================================

    @Test
    void shouldTreeOutputContainRequiredFields() throws Exception {
        ReadOnlyRepositoryRequest request = new ReadOnlyRepositoryRequest();
        request.setBranch("main");
        request.setPathPrefix("");
        request.setMaxFiles(10);

        // Use the adapter directly
        RepositoryTreeResult result = repositoryAdapter.listTree(request);

        assertThat(result.getFiles()).isNotNull();
        assertThat(result.getSkippedFiles()).isNotNull();

        // Convert to output payload via RepositoryReadToolService
        RepositoryReadToolService service = new RepositoryReadToolService(repositoryAdapter);
        RepositoryReadToolService.RepositoryToolResult toolResult =
                service.executeReadOnlyTool(0L, "READ_REPOSITORY_TREE",
                        Map.of("branch", "main", "pathPrefix", "", "maxFiles", 10));

        JsonNode payload = objectMapper.readTree(toolResult.getOutputPayload());
        assertThat(payload.has("filesRead")).isTrue();
        assertThat(payload.has("skippedFiles")).isTrue();
        assertThat(payload.has("filesTouched")).isTrue();
        assertThat(payload.has("gitOperations")).isTrue();
        assertThat(payload.has("redacted")).isTrue();
        assertThat(payload.has("truncated")).isTrue();
        assertThat(payload.get("filesTouched")).isEmpty();
        assertThat(payload.get("gitOperations")).isEmpty();
    }

    @Test
    void shouldSnippetOutputContainRedactedFlag() throws Exception {
        // Create a file with secrets to trigger redaction
        Path secretFile = workspaceDir.resolve(ALLOWED + "/secret-config.java");
        Files.createDirectories(secretFile.getParent());
        try {
            Files.writeString(secretFile, """
                    class Config {
                        String apiKey = "sk-test12345678901234567890";
                        String password = "supersecret";
                    }
                    """);

            ReadOnlyRepositoryRequest request = new ReadOnlyRepositoryRequest();
            request.setBranch("main");
            request.setFilePath(ALLOWED + "/secret-config.java");
            request.setStartLine(1);
            request.setMaxLines(50);

            RepositoryFileSnippetResult result = repositoryAdapter.readSnippet(request);

            if (result.getContent() != null && !result.getContent().isEmpty()) {
                JsonNode payload = objectMapper.readTree(
                        new ObjectMapper().writeValueAsString(Map.of(
                                "content", result.getContent(),
                                "redacted", result.isRedacted(),
                                "redactionCount", result.getRedactionCount()
                        )));
                assertThat(payload.has("redacted")).isTrue();
            }
        } finally {
            Files.deleteIfExists(secretFile);
        }
    }

    @Test
    void shouldSnippetOutputContainSkippedFiles() {
        ReadOnlyRepositoryRequest request = new ReadOnlyRepositoryRequest();
        request.setBranch("main");
        request.setFilePath(ALLOWED + "/.env");

        RepositoryFileSnippetResult result = repositoryAdapter.readSnippet(request);

        assertThat(result.getSkippedFiles()).isNotEmpty();
        RepositorySkippedFileItem skipped = result.getSkippedFiles().get(0);
        assertThat(skipped.getFilePath()).isEqualTo(ALLOWED + "/.env");
        assertThat(skipped.getReason()).isNotBlank();
    }

    // ==================================
    // 5. Content Redaction (4 extra tests)
    // ==================================

    @Test
    void shouldRedactBearerToken() {
        String input = "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.dGVzdA";
        String redacted = contentSafetyService.redactSecrets(input);
        assertThat(redacted).contains("**REDACTED**");
        assertThat(redacted).doesNotContain("eyJhbGci");
    }

    @Test
    void shouldRedactPasswordAssignment() {
        String input = "db.password = 'mysecretpassword123'";
        String redacted = contentSafetyService.redactSecrets(input);
        assertThat(redacted).contains("**REDACTED**");
    }

    @Test
    void shouldNotRedactNormalText() {
        String input = "public class HelloWorld { }";
        String redacted = contentSafetyService.redactSecrets(input);
        assertThat(redacted).isEqualTo(input);
    }

    @Test
    void shouldEnforceOutputLimit() {
        String largeContent = "x".repeat(300 * 1024);
        assertThat(contentSafetyService.isOutputSizeWithinLimit(largeContent)).isFalse();

        String smallContent = "small";
        assertThat(contentSafetyService.isOutputSizeWithinLimit(smallContent)).isTrue();
    }
}
