package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.CodeIndexChunkEntity;
import com.aicoding.platform.orchestration.domain.CodeIndexFileEntity;
import com.aicoding.platform.orchestration.domain.CodeIndexFileStatus;
import com.aicoding.platform.orchestration.domain.CodeIndexSymbolEntity;
import com.aicoding.platform.orchestration.dto.CodeIndexFileResponse;
import com.aicoding.platform.orchestration.dto.CodeIndexSummaryResponse;
import com.aicoding.platform.orchestration.dto.CodeIndexSymbolResponse;
import com.aicoding.platform.orchestration.dto.ReadOnlyRepositoryRequest;
import com.aicoding.platform.orchestration.dto.RepositoryFileSnippetResult;
import com.aicoding.platform.orchestration.dto.RepositoryReadFileItem;
import com.aicoding.platform.orchestration.dto.RepositoryTreeResult;
import com.aicoding.platform.orchestration.infrastructure.CodeIndexChunkMapper;
import com.aicoding.platform.orchestration.infrastructure.CodeIndexFileMapper;
import com.aicoding.platform.orchestration.infrastructure.CodeIndexSymbolMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CodeIndexBuildService {

    private static final Logger log = LoggerFactory.getLogger(CodeIndexBuildService.class);

    private static final int DEFAULT_MAX_FILES = 100;
    private static final int MAX_FILES_LIMIT = 500;
    private static final int CHUNK_LINE_SIZE = 50;

    private final CodeIndexFileMapper codeIndexFileMapper;
    private final CodeIndexSymbolMapper codeIndexSymbolMapper;
    private final CodeIndexChunkMapper codeIndexChunkMapper;
    private final CodeSymbolExtractorService symbolExtractorService;
    private final RepositoryToolSafetyService safetyService;
    private final ReadOnlyRepositoryAdapter repositoryAdapter;

    public CodeIndexBuildService(CodeIndexFileMapper codeIndexFileMapper,
                                  CodeIndexSymbolMapper codeIndexSymbolMapper,
                                  CodeIndexChunkMapper codeIndexChunkMapper,
                                  CodeSymbolExtractorService symbolExtractorService,
                                  RepositoryToolSafetyService safetyService,
                                  ReadOnlyRepositoryAdapter repositoryAdapter) {
        this.codeIndexFileMapper = codeIndexFileMapper;
        this.codeIndexSymbolMapper = codeIndexSymbolMapper;
        this.codeIndexChunkMapper = codeIndexChunkMapper;
        this.symbolExtractorService = symbolExtractorService;
        this.safetyService = safetyService;
        this.repositoryAdapter = repositoryAdapter;
    }

    @Transactional
    public CodeIndexSummaryResponse buildIndex(Long projectId, String branch, String pathPrefix, Integer maxFiles) {
        if (maxFiles == null || maxFiles <= 0) maxFiles = DEFAULT_MAX_FILES;
        if (maxFiles > MAX_FILES_LIMIT) maxFiles = MAX_FILES_LIMIT;

        String safeBranch = branch != null && !branch.isBlank() ? branch : "main";

        // Read file tree from adapter (real workspace scan)
        ReadOnlyRepositoryRequest treeRequest = new ReadOnlyRepositoryRequest();
        treeRequest.setBranch(safeBranch);
        treeRequest.setPathPrefix(pathPrefix != null ? pathPrefix : "");
        treeRequest.setMaxFiles(maxFiles);

        RepositoryTreeResult treeResult = repositoryAdapter.listTree(treeRequest);

        LocalDateTime now = LocalDateTime.now();
        int fileCount = 0;
        int symbolCount = 0;
        int chunkCount = 0;

        for (RepositoryReadFileItem fileItem : treeResult.getFiles()) {
            if (fileCount >= maxFiles) break;

            String filePath = fileItem.getFilePath();
            String language = fileItem.getLanguage();
            if (language == null || language.isBlank()) continue;

            try {
                String normalizedPath = safetyService.normalizeRelativePath(filePath);
                safetyService.validateSafeRelativePath(normalizedPath);

                // Read file content through adapter
                ReadOnlyRepositoryRequest snippetRequest = new ReadOnlyRepositoryRequest();
                snippetRequest.setBranch(safeBranch);
                snippetRequest.setFilePath(filePath);
                snippetRequest.setStartLine(1);
                snippetRequest.setMaxLines(99999);

                RepositoryFileSnippetResult snippetResult = repositoryAdapter.readSnippet(snippetRequest);
                String fileContent = snippetResult.getContent();

                if (fileContent == null || fileContent.isBlank()) continue;

                int lineCount = fileContent.split("\n", -1).length;

                // Upsert file record
                CodeIndexFileEntity fileEntity = upsertFileRecord(projectId, safeBranch, filePath,
                        language, fileContent, lineCount, now);
                if (fileEntity == null) continue;

                // Delete old symbols and chunks for this file
                codeIndexSymbolMapper.delete(
                        new LambdaQueryWrapper<CodeIndexSymbolEntity>()
                                .eq(CodeIndexSymbolEntity::getFileId, fileEntity.getId()));
                codeIndexChunkMapper.delete(
                        new LambdaQueryWrapper<CodeIndexChunkEntity>()
                                .eq(CodeIndexChunkEntity::getFileId, fileEntity.getId()));

                // Extract symbols from redacted content
                List<CodeSymbolExtractorService.SymbolExtraction> symbols =
                        symbolExtractorService.extractSymbols(language, filePath,
                                fileContent, lineCount);
                for (CodeSymbolExtractorService.SymbolExtraction sym : symbols) {
                    CodeIndexSymbolEntity symEntity = new CodeIndexSymbolEntity();
                    symEntity.setProjectId(projectId);
                    symEntity.setFileId(fileEntity.getId());
                    symEntity.setSymbolName(sym.getSymbolName());
                    symEntity.setSymbolType(sym.getSymbolType());
                    symEntity.setLanguage(language);
                    symEntity.setFilePath(filePath);
                    symEntity.setStartLine(sym.getStartLine() > 0 ? sym.getStartLine() : null);
                    symEntity.setEndLine(sym.getEndLine() > 0 ? sym.getEndLine() : null);
                    symEntity.setSnippet(sym.getSnippet() != null && sym.getSnippet().length() <= 500
                            ? sym.getSnippet() : null);
                    codeIndexSymbolMapper.insert(symEntity);
                    symbolCount++;
                }

                // Create chunks from redacted content
                List<CodeIndexChunkEntity> chunks = createChunks(projectId, fileEntity,
                        filePath, fileContent);
                for (CodeIndexChunkEntity chunk : chunks) {
                    codeIndexChunkMapper.insert(chunk);
                    chunkCount++;
                }

                fileCount++;
            } catch (IllegalArgumentException e) {
                log.debug("Skipping file due to safety check: {} - {}", filePath, e.getMessage());
            }
        }

        // Fallback to mock generation when workspace has no files
        boolean usedMockFallback = false;
        if (fileCount == 0) {
            usedMockFallback = true;
            log.info("Workspace returned no files, falling back to mock generation for projectId={}", projectId);
            List<MockFileEntry> mockFiles = generateMockFileList(pathPrefix, maxFiles);
            for (MockFileEntry entry : mockFiles) {
                try {
                    String normalizedPath = safetyService.normalizeRelativePath(entry.filePath);
                    safetyService.validateSafeRelativePath(normalizedPath);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                CodeIndexFileEntity fileEntity = upsertFileRecord(projectId, safeBranch, entry.filePath,
                        entry.language, entry.mockContent, entry.lineCount, now);
                if (fileEntity == null) continue;

                codeIndexSymbolMapper.delete(
                        new LambdaQueryWrapper<CodeIndexSymbolEntity>()
                                .eq(CodeIndexSymbolEntity::getFileId, fileEntity.getId()));
                codeIndexChunkMapper.delete(
                        new LambdaQueryWrapper<CodeIndexChunkEntity>()
                                .eq(CodeIndexChunkEntity::getFileId, fileEntity.getId()));

                List<CodeSymbolExtractorService.SymbolExtraction> symbols =
                        symbolExtractorService.extractSymbols(entry.language, entry.filePath,
                                entry.mockContent, entry.lineCount);
                for (CodeSymbolExtractorService.SymbolExtraction sym : symbols) {
                    CodeIndexSymbolEntity symEntity = new CodeIndexSymbolEntity();
                    symEntity.setProjectId(projectId);
                    symEntity.setFileId(fileEntity.getId());
                    symEntity.setSymbolName(sym.getSymbolName());
                    symEntity.setSymbolType(sym.getSymbolType());
                    symEntity.setLanguage(entry.language);
                    symEntity.setFilePath(entry.filePath);
                    symEntity.setStartLine(sym.getStartLine() > 0 ? sym.getStartLine() : null);
                    symEntity.setEndLine(sym.getEndLine() > 0 ? sym.getEndLine() : null);
                    symEntity.setSnippet(sym.getSnippet() != null && sym.getSnippet().length() <= 500
                            ? sym.getSnippet() : null);
                    codeIndexSymbolMapper.insert(symEntity);
                    symbolCount++;
                }

                List<CodeIndexChunkEntity> chunks = createChunks(projectId, fileEntity,
                        entry.filePath, entry.mockContent);
                for (CodeIndexChunkEntity chunk : chunks) {
                    codeIndexChunkMapper.insert(chunk);
                    chunkCount++;
                }
                fileCount++;
            }
            log.info("Mock fallback complete for projectId={}: {} files, {} symbols, {} chunks",
                    projectId, fileCount, symbolCount, chunkCount);
        }

        log.info("Code index build complete for projectId={}: {} files, {} symbols, {} chunks (redacted={})",
                projectId, fileCount, symbolCount, chunkCount, treeResult.isRedacted());

        CodeIndexSummaryResponse summary = new CodeIndexSummaryResponse();
        summary.setProjectId(projectId.toString());
        summary.setFileCount(fileCount);
        summary.setSymbolCount(symbolCount);
        summary.setChunkCount(chunkCount);
        summary.setIndexedAt(now.toString());
        summary.setMock(usedMockFallback);
        return summary;
    }

    public CodeIndexSummaryResponse getSummary(Long projectId) {
        int fileCount = codeIndexFileMapper.selectCount(
                new LambdaQueryWrapper<CodeIndexFileEntity>()
                        .eq(CodeIndexFileEntity::getProjectId, projectId)).intValue();
        int symbolCount = codeIndexSymbolMapper.selectCount(
                new LambdaQueryWrapper<CodeIndexSymbolEntity>()
                        .eq(CodeIndexSymbolEntity::getProjectId, projectId)).intValue();
        int chunkCount = codeIndexChunkMapper.selectCount(
                new LambdaQueryWrapper<CodeIndexChunkEntity>()
                        .eq(CodeIndexChunkEntity::getProjectId, projectId)).intValue();

        CodeIndexSummaryResponse summary = new CodeIndexSummaryResponse();
        summary.setProjectId(projectId.toString());
        summary.setFileCount(fileCount);
        summary.setSymbolCount(symbolCount);
        summary.setChunkCount(chunkCount);
        summary.setIndexedAt(LocalDateTime.now().toString());

        if (fileCount > 0) {
            List<CodeIndexFileEntity> files = codeIndexFileMapper.selectList(
                    new LambdaQueryWrapper<CodeIndexFileEntity>()
                            .eq(CodeIndexFileEntity::getProjectId, projectId)
                            .orderByDesc(CodeIndexFileEntity::getIndexedAt)
                            .last("LIMIT 1"));
            if (!files.isEmpty() && files.get(0).getIndexedAt() != null) {
                summary.setIndexedAt(files.get(0).getIndexedAt().toString());
            }
        }

        summary.setMock(false);
        return summary;
    }

    public List<CodeIndexFileResponse> listFiles(Long projectId, String branch, String pathPrefix, int limit) {
        LambdaQueryWrapper<CodeIndexFileEntity> wrapper = new LambdaQueryWrapper<CodeIndexFileEntity>()
                .eq(CodeIndexFileEntity::getProjectId, projectId);
        if (branch != null && !branch.isBlank()) {
            wrapper.eq(CodeIndexFileEntity::getBranch, branch);
        }
        if (pathPrefix != null && !pathPrefix.isBlank()) {
            wrapper.like(CodeIndexFileEntity::getFilePath, pathPrefix);
        }
        wrapper.orderByAsc(CodeIndexFileEntity::getFilePath)
                .last("LIMIT " + Math.min(limit, 200));

        return codeIndexFileMapper.selectList(wrapper).stream()
                .map(this::toFileResponse)
                .collect(Collectors.toList());
    }

    public List<CodeIndexSymbolResponse> listSymbols(Long projectId, String branch, String symbolType, int limit) {
        LambdaQueryWrapper<CodeIndexSymbolEntity> wrapper = new LambdaQueryWrapper<CodeIndexSymbolEntity>()
                .eq(CodeIndexSymbolEntity::getProjectId, projectId);
        if (symbolType != null && !symbolType.isBlank()) {
            wrapper.eq(CodeIndexSymbolEntity::getSymbolType, symbolType);
        }
        wrapper.orderByAsc(CodeIndexSymbolEntity::getSymbolName)
                .last("LIMIT " + Math.min(limit, 200));

        return codeIndexSymbolMapper.selectList(wrapper).stream()
                .map(this::toSymbolResponse)
                .collect(Collectors.toList());
    }

    // ========================
    // Internal helpers
    // ========================

    private CodeIndexFileEntity upsertFileRecord(Long projectId, String branch, String filePath,
                                                   String language, String content, int lineCount,
                                                   LocalDateTime now) {
        try {
            String contentHash = sha256(content);

            CodeIndexFileEntity existing = codeIndexFileMapper.selectOne(
                    new LambdaQueryWrapper<CodeIndexFileEntity>()
                            .eq(CodeIndexFileEntity::getProjectId, projectId)
                            .eq(CodeIndexFileEntity::getBranch, branch)
                            .eq(CodeIndexFileEntity::getFilePath, filePath));

            if (existing != null && contentHash.equals(existing.getContentHash())) {
                existing.setIndexedAt(now);
                existing.setStatus(CodeIndexFileStatus.INDEXED.name());
                codeIndexFileMapper.updateById(existing);
                return existing;
            }

            CodeIndexFileEntity fileEntity = existing != null ? existing : new CodeIndexFileEntity();
            fileEntity.setProjectId(projectId);
            fileEntity.setBranch(branch);
            fileEntity.setFilePath(filePath);
            fileEntity.setLanguage(language);
            fileEntity.setFileSize((long) content.length());
            fileEntity.setLineCount(lineCount);
            fileEntity.setContentHash(contentHash);
            fileEntity.setIndexedAt(now);
            fileEntity.setStatus(CodeIndexFileStatus.INDEXED.name());

            if (existing != null) {
                codeIndexFileMapper.updateById(fileEntity);
            } else {
                codeIndexFileMapper.insert(fileEntity);
            }

            return fileEntity;
        } catch (Exception e) {
            log.warn("Failed to upsert file record: {} - {}", filePath, e.getMessage());
            return null;
        }
    }

    private List<CodeIndexChunkEntity> createChunks(Long projectId, CodeIndexFileEntity fileEntity,
                                                      String filePath, String content) {
        List<CodeIndexChunkEntity> chunks = new ArrayList<>();
        if (content == null || content.isBlank()) return chunks;

        String[] lines = content.split("\n", -1);
        int numChunks = (int) Math.ceil((double) lines.length / CHUNK_LINE_SIZE);

        for (int i = 0; i < numChunks; i++) {
            int startLine = i * CHUNK_LINE_SIZE + 1;
            int endLine = Math.min((i + 1) * CHUNK_LINE_SIZE, lines.length);
            StringBuilder chunkContent = new StringBuilder();
            for (int j = (i * CHUNK_LINE_SIZE); j < endLine; j++) {
                chunkContent.append(lines[j]).append("\n");
            }

            CodeIndexChunkEntity chunk = new CodeIndexChunkEntity();
            chunk.setProjectId(projectId);
            chunk.setFileId(fileEntity.getId());
            chunk.setFilePath(filePath);
            chunk.setChunkIndex(i);
            chunk.setStartLine(startLine);
            chunk.setEndLine(endLine);
            chunk.setContent(chunkContent.toString());
            chunk.setTokenCount(chunkContent.length() / 5);
            try {
                chunk.setContentHash(sha256(chunkContent.toString()));
            } catch (Exception e) {
                chunk.setContentHash(String.valueOf(chunkContent.toString().hashCode()));
            }
            chunks.add(chunk);
        }

        return chunks;
    }

    private String sha256(String content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private CodeIndexFileResponse toFileResponse(CodeIndexFileEntity entity) {
        CodeIndexFileResponse resp = new CodeIndexFileResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setFilePath(entity.getFilePath());
        resp.setLanguage(entity.getLanguage());
        resp.setFileSize(entity.getFileSize());
        resp.setLineCount(entity.getLineCount());
        resp.setStatus(entity.getStatus());
        resp.setIndexedAt(entity.getIndexedAt() != null ? entity.getIndexedAt().toString() : null);
        return resp;
    }

    private CodeIndexSymbolResponse toSymbolResponse(CodeIndexSymbolEntity entity) {
        CodeIndexSymbolResponse resp = new CodeIndexSymbolResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setFileId(entity.getFileId().toString());
        resp.setSymbolName(entity.getSymbolName());
        resp.setSymbolType(entity.getSymbolType());
        resp.setLanguage(entity.getLanguage());
        resp.setFilePath(entity.getFilePath());
        resp.setStartLine(entity.getStartLine());
        resp.setEndLine(entity.getEndLine());
        resp.setSnippet(entity.getSnippet());
        return resp;
    }

    // ========================
    // Mock fallback methods (used when workspace is empty)
    // ========================

    private List<MockFileEntry> generateMockFileList(String pathPrefix, int maxFiles) {
        List<MockFileEntry> files = new ArrayList<>();
        String prefix = pathPrefix != null && !pathPrefix.isBlank() ? pathPrefix : "";

        String[][] mockEntries = {
                {"src/main/java/com/example/Application.java", "java", "200"},
                {"src/main/java/com/example/controller/ApiController.java", "java", "150"},
                {"src/main/java/com/example/service/UserService.java", "java", "180"},
                {"src/main/java/com/example/service/TaskService.java", "java", "120"},
                {"src/main/java/com/example/repository/UserRepository.java", "java", "60"},
                {"src/main/java/com/example/config/SecurityConfig.java", "java", "80"},
                {"src/main/java/com/example/model/UserEntity.java", "java", "90"},
                {"src/main/java/com/example/model/TaskEntity.java", "java", "85"},
                {"src/main/java/com/example/util/DateUtils.java", "java", "40"},
                {"src/main/java/com/example/exception/GlobalExceptionHandler.java", "java", "55"},
                {"src/main/resources/application.yml", "yaml", "50"},
                {"src/main/resources/db/migration/V1__init.sql", "sql", "100"},
                {"src/test/java/com/example/ApplicationTest.java", "java", "45"},
                {"src/test/java/com/example/service/UserServiceTest.java", "java", "120"},
                {"src/test/java/com/example/service/TaskServiceTest.java", "java", "95"},
                {"src/main/java/com/example/dto/UserDto.java", "java", "35"},
                {"src/main/java/com/example/dto/TaskDto.java", "java", "35"},
                {"src/main/java/com/example/mapper/UserMapper.java", "java", "25"},
                {"src/main/java/com/example/mapper/TaskMapper.java", "java", "25"},
                {"src/main/java/com/example/service/impl/UserServiceImpl.java", "java", "100"},
        };

        int count = 0;
        for (String[] entry : mockEntries) {
            if (count >= maxFiles) break;
            String fullPath = prefix.isEmpty() ? entry[0] : prefix + "/" + entry[0];
            try {
                safetyService.validateSafeRelativePath(fullPath);
            } catch (IllegalArgumentException e) {
                continue;
            }

            int lineCount = Integer.parseInt(entry[2]);
            files.add(new MockFileEntry(fullPath, detectLanguage(fullPath),
                    generateMockContent(entry[1], lineCount), lineCount));
            count++;
        }

        if (prefix.isEmpty() || prefix.contains("frontend") || prefix.contains("web")) {
            String[][] feEntries = {
                    {"src/components/AppHeader.vue", "vue", "80"},
                    {"src/components/UserProfile.ts", "ts", "60"},
                    {"src/pages/HomePage.tsx", "tsx", "70"},
                    {"src/utils/api.ts", "ts", "50"},
                    {"src/store/userStore.ts", "ts", "90"},
            };
            for (String[] entry : feEntries) {
                if (count >= maxFiles) break;
                String fullPath = prefix.isEmpty() ? entry[0] : prefix + "/" + entry[0];
                try {
                    safetyService.validateSafeRelativePath(fullPath);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                int lineCount = Integer.parseInt(entry[2]);
                files.add(new MockFileEntry(fullPath, detectLanguage(fullPath),
                        generateMockContent(entry[1], lineCount), lineCount));
                count++;
            }
        }

        return files;
    }

    private String detectLanguage(String path) {
        if (path == null) return "";
        String lower = path.toLowerCase();
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".ts") || lower.endsWith(".tsx")) return "ts";
        if (lower.endsWith(".js") || lower.endsWith(".jsx")) return "js";
        if (lower.endsWith(".vue")) return "vue";
        if (lower.endsWith(".sql")) return "sql";
        if (lower.endsWith(".md")) return "md";
        if (lower.endsWith(".kt")) return "kotlin";
        if (lower.endsWith(".py")) return "python";
        if (lower.endsWith(".go")) return "go";
        if (lower.endsWith(".rs")) return "rust";
        if (lower.endsWith(".xml") || lower.endsWith(".html") || lower.endsWith(".htm")) return "xml";
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) return "yaml";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".css") || lower.endsWith(".scss") || lower.endsWith(".less")) return "css";
        return "";
    }

    private String generateMockContent(String language, int lineCount) {
        String preamble = buildContentPreamble(language);
        String[] preambleLines = preamble.split("\n", -1);
        int fillerLines = Math.max(0, lineCount - preambleLines.length);
        StringBuilder sb = new StringBuilder(preamble);
        for (int i = 0; i < fillerLines; i++) {
            sb.append("// Line ").append(i + 1).append(" of mock ").append(language).append(" content\n");
        }
        return sb.toString();
    }

    private String buildContentPreamble(String language) {
        return switch (language != null ? language.toLowerCase() : "") {
            case "java" -> """
                    package com.example;

                    import java.util.List;

                    public class MockClass {

                        private String name;

                        public MockClass() {}

                        public String getName() { return name; }

                        public void setName(String name) { this.name = name; }

                        public void process() {
                            // processing logic
                        }

                    }
                    """;
            case "ts", "tsx" -> """
                    import { Component } from 'react'

                    interface MockProps {
                      title: string;
                    }

                    function MockFunction(props: MockProps) {
                      return null;
                    }

                    export default MockFunction;
                    """;
            case "vue" -> """
                    <script setup lang="ts">
                    import { ref } from 'vue'

                    const count = ref(0)
                    </script>

                    <template>
                      <div>Mock Component</div>
                    </template>
                    """;
            case "sql" -> """
                    CREATE TABLE mock_table (
                      id BIGINT PRIMARY KEY,
                      name VARCHAR(100)
                    );

                    CREATE INDEX idx_mock_name ON mock_table(name);
                    """;
            case "md" -> """
                    # Mock Document

                    ## Section 1

                    Some content here.

                    ## Section 2

                    More content.
                    """;
            default -> "# Mock content\n\n";
        };
    }

    private static class MockFileEntry {
        final String filePath;
        final String language;
        final String mockContent;
        final int lineCount;

        MockFileEntry(String filePath, String language, String mockContent, int lineCount) {
            this.filePath = filePath;
            this.language = language;
            this.mockContent = mockContent;
            this.lineCount = lineCount;
        }
    }
}
