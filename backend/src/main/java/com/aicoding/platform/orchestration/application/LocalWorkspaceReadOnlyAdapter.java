package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.config.ReadOnlyToolProperties;
import com.aicoding.platform.orchestration.dto.ReadOnlyRepositoryRequest;
import com.aicoding.platform.orchestration.dto.RepositoryBranchResult;
import com.aicoding.platform.orchestration.dto.RepositoryDiffSummaryResult;
import com.aicoding.platform.orchestration.dto.RepositoryFileSnippetResult;
import com.aicoding.platform.orchestration.dto.RepositoryReadFileItem;
import com.aicoding.platform.orchestration.dto.RepositorySkippedFileItem;
import com.aicoding.platform.orchestration.dto.RepositoryTreeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@Component
public class LocalWorkspaceReadOnlyAdapter implements ReadOnlyRepositoryAdapter {

    private static final Logger log = LoggerFactory.getLogger(LocalWorkspaceReadOnlyAdapter.class);

    private final RepositoryToolSafetyService safetyService;
    private final RepositoryContentSafetyService contentSafetyService;
    private final ReadOnlyToolProperties properties;
    private final Path workspaceRoot;

    private static final String LANGUAGE_JAVA = "java";
    private static final String LANGUAGE_TS = "ts";
    private static final String LANGUAGE_JS = "js";
    private static final String LANGUAGE_VUE = "vue";
    private static final String LANGUAGE_SQL = "sql";
    private static final String LANGUAGE_MD = "md";
    private static final String LANGUAGE_KOTLIN = "kotlin";
    private static final String LANGUAGE_PYTHON = "python";
    private static final String LANGUAGE_GO = "go";
    private static final String LANGUAGE_RUST = "rust";
    private static final String LANGUAGE_XML = "xml";
    private static final String LANGUAGE_YAML = "yaml";
    private static final String LANGUAGE_JSON = "json";
    private static final String LANGUAGE_CSS = "css";
    private static final String SKIP_SENSITIVE_PATH = "SENSITIVE_PATH";
    private static final String SKIP_BINARY_FILE = "BINARY_FILE";
    private static final String SKIP_OUTSIDE_ALLOWED = "OUTSIDE_ALLOWED_PREFIX";
    private static final String SKIP_READ_ERROR = "READ_ERROR";

    public LocalWorkspaceReadOnlyAdapter(RepositoryToolSafetyService safetyService,
                                          RepositoryContentSafetyService contentSafetyService,
                                          ReadOnlyToolProperties properties,
                                          @Value("${app.workspace.root-path}") String workspaceRootPath) {
        this.safetyService = safetyService;
        this.contentSafetyService = contentSafetyService;
        this.properties = properties;
        this.workspaceRoot = Paths.get(workspaceRootPath).normalize().toAbsolutePath();
    }

    @Override
    public RepositoryTreeResult listTree(ReadOnlyRepositoryRequest request) {
        RepositoryTreeResult result = new RepositoryTreeResult();
        String branch = request.getBranch() != null ? request.getBranch() : "main";
        result.setBranch(branch);

        String pathPrefix = request.getPathPrefix() != null ? request.getPathPrefix() : "";
        result.setPathPrefix(pathPrefix);

        if (!workspaceExists()) {
            log.warn("Workspace root does not exist: {}, falling back to empty tree", workspaceRoot);
            result.setFiles(List.of());
            result.setSkippedFiles(List.of());
            result.setTruncated(false);
            result.setRedacted(false);
            return result;
        }

        Integer reqMaxFiles = request.getMaxFiles();
        int maxFiles = reqMaxFiles != null ? reqMaxFiles : properties.getMaxFiles();

        boolean globalTruncated = false;
        boolean globalRedacted = false;
        List<RepositoryReadFileItem> files = new ArrayList<>();
        List<RepositorySkippedFileItem> skippedFiles = new ArrayList<>();

        Path searchRoot = resolveSearchRoot(pathPrefix);

        if (!Files.exists(searchRoot) || !Files.isDirectory(searchRoot)) {
            result.setFiles(List.of());
            result.setSkippedFiles(List.of());
            result.setTruncated(false);
            result.setRedacted(false);
            return result;
        }

        try {
            List<Path> collectedFiles = new ArrayList<>();
            collectFiles(searchRoot, collectedFiles, maxFiles);

            for (Path filePath : collectedFiles) {
                if (files.size() >= maxFiles) {
                    globalTruncated = true;
                    break;
                }

                String relativePath = relativizePath(filePath);
                if (relativePath == null) continue;

                RepositorySkippedFileItem skip = checkPathSafety(relativePath);
                if (skip != null) {
                    skippedFiles.add(skip);
                    continue;
                }

                try {
                    BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
                    long fileSize = attrs.size();

                    boolean isBinary;
                    try {
                        byte[] probe = Files.readAllBytes(filePath);
                        isBinary = contentSafetyService.isBinaryContent(probe)
                                || contentSafetyService.isBinaryExtension(relativePath);
                    } catch (IOException e) {
                        skippedFiles.add(new RepositorySkippedFileItem(relativePath, SKIP_READ_ERROR));
                        continue;
                    }

                    if (isBinary) {
                        skippedFiles.add(new RepositorySkippedFileItem(relativePath, SKIP_BINARY_FILE));
                        continue;
                    }

                    String content;
                    try {
                        content = Files.readString(filePath, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        skippedFiles.add(new RepositorySkippedFileItem(relativePath, SKIP_READ_ERROR));
                        continue;
                    }

                    String contentHash = sha256(content);
                    int lineCount = content.isEmpty() ? 0 : content.split("\n", -1).length;

                    boolean fileRedacted = false;
                    if (properties.isRedactionEnabled()) {
                        RepositoryContentSafetyService.RedactionResult redacted = contentSafetyService.redactSecretsWithCount(content);
                        if (redacted.getRedactionCount() > 0) {
                            fileRedacted = true;
                            globalRedacted = true;
                        }
                    }

                    RepositoryReadFileItem fileItem = new RepositoryReadFileItem();
                    fileItem.setFilePath(relativePath);
                    fileItem.setLanguage(detectLanguage(relativePath));
                    fileItem.setFileSize(fileSize);
                    fileItem.setLineCount(lineCount);
                    fileItem.setContentHash(contentHash);
                    fileItem.setTruncated(false);
                    fileItem.setRedacted(fileRedacted);
                    files.add(fileItem);

                } catch (IOException e) {
                    skippedFiles.add(new RepositorySkippedFileItem(relativePath, SKIP_READ_ERROR));
                }
            }
        } catch (IOException e) {
            log.warn("Error walking workspace tree: {}", e.getMessage());
        }

        result.setFiles(files);
        result.setSkippedFiles(skippedFiles);
        result.setTruncated(globalTruncated);
        result.setRedacted(globalRedacted);
        return result;
    }

    @Override
    public RepositoryFileSnippetResult readSnippet(ReadOnlyRepositoryRequest request) {
        RepositoryFileSnippetResult result = new RepositoryFileSnippetResult();
        String branch = request.getBranch() != null ? request.getBranch() : "main";
        result.setBranch(branch);

        String filePath = request.getFilePath() != null ? request.getFilePath() : "";
        result.setFilePath(filePath);

        if (filePath.isBlank()) {
            result.setContent("");
            result.setSkippedFiles(List.of(new RepositorySkippedFileItem("", "FILE_PATH_EMPTY")));
            result.setTruncated(false);
            result.setRedacted(false);
            result.setRedactionCount(0);
            return result;
        }

        // Check path safety
        RepositorySkippedFileItem skip = checkPathSafety(filePath);
        if (skip != null) {
            result.setContent("");
            result.setSkippedFiles(List.of(skip));
            result.setTruncated(false);
            result.setRedacted(false);
            result.setRedactionCount(0);
            result.setLanguage(detectLanguage(filePath));
            return result;
        }

        if (!workspaceExists()) {
            log.warn("Workspace root does not exist: {}, returning empty snippet", workspaceRoot);
            result.setContent("");
            result.setSkippedFiles(List.of());
            result.setTruncated(false);
            result.setRedacted(false);
            result.setRedactionCount(0);
            result.setLanguage(detectLanguage(filePath));
            return result;
        }

        try {
            Path resolved;
            try {
                resolved = safetyService.resolveAndValidatePath(workspaceRoot, filePath);
            } catch (IllegalArgumentException e) {
                result.setContent("");
                result.setSkippedFiles(List.of(new RepositorySkippedFileItem(filePath, SKIP_OUTSIDE_ALLOWED)));
                result.setTruncated(false);
                result.setRedacted(false);
                result.setRedactionCount(0);
                result.setLanguage(detectLanguage(filePath));
                return result;
            }

            if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
                result.setContent("");
                result.setSkippedFiles(List.of(new RepositorySkippedFileItem(filePath, "FILE_NOT_FOUND")));
                result.setTruncated(false);
                result.setRedacted(false);
                result.setRedactionCount(0);
                result.setLanguage(detectLanguage(filePath));
                return result;
            }

            BasicFileAttributes attrs = Files.readAttributes(resolved, BasicFileAttributes.class);
            result.setTotalLines((int) (attrs.size() / 40) + 1);

            // Check binary
            byte[] rawBytes = Files.readAllBytes(resolved);
            if (contentSafetyService.isBinaryContent(rawBytes) || contentSafetyService.isBinaryExtension(filePath)) {
                result.setContent("");
                result.setSkippedFiles(List.of(new RepositorySkippedFileItem(filePath, SKIP_BINARY_FILE)));
                result.setTruncated(false);
                result.setRedacted(false);
                result.setRedactionCount(0);
                result.setLanguage(detectLanguage(filePath));
                return result;
            }

            String fullContent = new String(rawBytes, StandardCharsets.UTF_8);
            String[] lines = fullContent.split("\n", -1);
            result.setTotalLines(lines.length);

            // Apply line range
            int startLine = request.getStartLine() != null ? Math.max(1, request.getStartLine()) : 1;
            int maxLines = request.getMaxLines() != null ? Math.max(1, request.getMaxLines()) : 80;
            int endLine = Math.min(startLine + maxLines - 1, lines.length);

            StringBuilder snippetBuilder = new StringBuilder();
            for (int i = startLine - 1; i < endLine; i++) {
                snippetBuilder.append(lines[i]).append("\n");
            }
            String snippetContent = snippetBuilder.toString();
            result.setStartLine(startLine);
            result.setEndLine(endLine);

            // Apply redaction
            String finalContent = snippetContent;
            int redactionCount = 0;
            boolean redacted = false;
            if (properties.isRedactionEnabled()) {
                RepositoryContentSafetyService.RedactionResult redactionResult =
                        contentSafetyService.redactSecretsWithCount(snippetContent);
                finalContent = redactionResult.getContent();
                redactionCount = redactionResult.getRedactionCount();
                redacted = redactionCount > 0;
            }

            // Check output size
            Long reqMaxBytes = request.getMaxBytes();
            long maxOutputBytes = reqMaxBytes != null ? reqMaxBytes : properties.getMaxOutputBytes();
            boolean truncated = false;
            if (finalContent.length() > maxOutputBytes) {
                finalContent = finalContent.substring(0, (int) maxOutputBytes);
                truncated = true;
            }

            result.setContent(finalContent);
            result.setRedacted(redacted);
            result.setRedactionCount(redactionCount);
            result.setTruncated(truncated);
            result.setSkippedFiles(List.of());
            result.setLanguage(detectLanguage(filePath));

        } catch (IOException e) {
            log.warn("Error reading file {}: {}", filePath, e.getMessage());
            result.setContent("");
            result.setSkippedFiles(List.of(new RepositorySkippedFileItem(filePath, SKIP_READ_ERROR)));
            result.setTruncated(false);
            result.setRedacted(false);
            result.setRedactionCount(0);
            result.setLanguage(detectLanguage(filePath));
        }

        return result;
    }

    @Override
    public RepositoryBranchResult listBranches(ReadOnlyRepositoryRequest request) {
        RepositoryBranchResult result = new RepositoryBranchResult();
        result.setIncludeRemote(request.isIncludeRemote());
        result.setNoCheckout(true);
        result.setNoPull(true);

        List<String> branches = new ArrayList<>();

        // Read local branches from .git/refs/heads/
        if (workspaceExists()) {
            Path gitHeads = workspaceRoot.resolve(".git/refs/heads/");
            if (Files.isDirectory(gitHeads)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(gitHeads)) {
                    for (Path headFile : stream) {
                        String branchName = headFile.getFileName().toString();
                        if (!branchName.startsWith(".")) {
                            branches.add(branchName);
                        }
                        // Also check subdirectories (e.g., feature/my-branch)
                        if (Files.isDirectory(headFile)) {
                            collectBranchNames(headFile, branchName, branches);
                        }
                    }
                } catch (IOException e) {
                    log.warn("Error reading .git/refs/heads: {}", e.getMessage());
                }
            }

            // Try to get current branch from HEAD
            try {
                Path headFile = workspaceRoot.resolve(".git/HEAD");
                if (Files.exists(headFile)) {
                    String headContent = Files.readString(headFile, StandardCharsets.UTF_8).trim();
                    if (headContent.startsWith("ref: refs/heads/")) {
                        String currentBranch = headContent.substring("ref: refs/heads/".length());
                        // Ensure current branch is first in the list
                        branches.remove(currentBranch);
                        branches.add(0, currentBranch);
                    }
                }
            } catch (IOException e) {
                log.warn("Error reading .git/HEAD: {}", e.getMessage());
            }
        }

        // Fallback to mock
        if (branches.isEmpty()) {
            branches.add("main");
        }

        // Add remote branches if requested
        if (request.isIncludeRemote()) {
            List<String> remoteBranches = new ArrayList<>();
            for (String b : branches) {
                if (!b.equals("main")) {
                    remoteBranches.add("remotes/origin/" + b);
                }
            }
            if (branches.contains("main")) {
                remoteBranches.add(0, "remotes/origin/main");
            }
            branches.addAll(remoteBranches);
        }

        result.setBranches(branches);
        return result;
    }

    @Override
    public RepositoryDiffSummaryResult readDiffSummary(ReadOnlyRepositoryRequest request) {
        // No real git diff execution — return mock with noRealGitDiff=true
        RepositoryDiffSummaryResult result = new RepositoryDiffSummaryResult();
        result.setBaseBranch(request.getBaseBranch() != null ? request.getBaseBranch() : "main");
        result.setTargetBranch(request.getBranch() != null ? request.getBranch() : "main");
        result.setFileCount(0);
        result.setAdditionCount(0);
        result.setDeletionCount(0);
        result.setChangedFiles(List.of());
        result.setTruncated(false);
        result.setNoRealGitDiff(true);
        return result;
    }

    // ====================
    // Internal helpers
    // ====================

    private boolean workspaceExists() {
        return Files.exists(workspaceRoot) && Files.isDirectory(workspaceRoot);
    }

    private Path resolveSearchRoot(String pathPrefix) {
        if (pathPrefix == null || pathPrefix.isBlank()) {
            return workspaceRoot;
        }
        String normalized = safetyService.normalizeRelativePath(pathPrefix);
        return workspaceRoot.resolve(normalized).normalize();
    }

    private String relativizePath(Path absolutePath) {
        try {
            Path relative = workspaceRoot.relativize(absolutePath);
            return relative.toString().replace("\\", "/");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Collect files recursively up to maxFiles.
     */
    private void collectFiles(Path dir, List<Path> results, int maxFiles) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (results.size() >= maxFiles) break;
                if (Files.isDirectory(entry)) {
                    collectFiles(entry, results, maxFiles);
                } else if (Files.isRegularFile(entry)) {
                    results.add(entry);
                }
            }
        }
    }

    /**
     * Check if a path passes safety checks. Returns skipped item if blocked.
     */
    private RepositorySkippedFileItem checkPathSafety(String relativePath) {
        try {
            safetyService.validateSafeRelativePath(relativePath);
            safetyService.validateAllowedPrefix(relativePath);
            return null;
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("敏感路径")) {
                return new RepositorySkippedFileItem(relativePath, SKIP_SENSITIVE_PATH);
            }
            if (msg != null && msg.contains("允许的访问前缀")) {
                return new RepositorySkippedFileItem(relativePath, SKIP_OUTSIDE_ALLOWED);
            }
            return new RepositorySkippedFileItem(relativePath, SKIP_SENSITIVE_PATH);
        }
    }

    private void collectBranchNames(Path dir, String parentName, List<String> results) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                String entryName = entry.getFileName().toString();
                String fullName = parentName + "/" + entryName;
                if (Files.isDirectory(entry)) {
                    collectBranchNames(entry, fullName, results);
                } else if (!entryName.startsWith(".")) {
                    results.add(fullName);
                }
            }
        } catch (IOException e) {
            log.debug("Error reading branch subdirectory {}: {}", dir, e.getMessage());
        }
    }

    private String detectLanguage(String filePath) {
        if (filePath == null) return "";
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".java")) return LANGUAGE_JAVA;
        if (lower.endsWith(".ts") || lower.endsWith(".tsx")) return LANGUAGE_TS;
        if (lower.endsWith(".js") || lower.endsWith(".jsx")) return LANGUAGE_JS;
        if (lower.endsWith(".vue")) return LANGUAGE_VUE;
        if (lower.endsWith(".sql")) return LANGUAGE_SQL;
        if (lower.endsWith(".md")) return LANGUAGE_MD;
        if (lower.endsWith(".kt")) return LANGUAGE_KOTLIN;
        if (lower.endsWith(".py")) return LANGUAGE_PYTHON;
        if (lower.endsWith(".go")) return LANGUAGE_GO;
        if (lower.endsWith(".rs")) return LANGUAGE_RUST;
        if (lower.endsWith(".xml") || lower.endsWith(".html") || lower.endsWith(".htm")) return LANGUAGE_XML;
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) return LANGUAGE_YAML;
        if (lower.endsWith(".json")) return LANGUAGE_JSON;
        if (lower.endsWith(".css") || lower.endsWith(".scss") || lower.endsWith(".less")) return LANGUAGE_CSS;
        return "";
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return String.valueOf(content.hashCode());
        }
    }
}
