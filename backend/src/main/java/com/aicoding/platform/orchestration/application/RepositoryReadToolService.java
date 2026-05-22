package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.ToolName;
import com.aicoding.platform.orchestration.dto.ReadOnlyRepositoryRequest;
import com.aicoding.platform.orchestration.dto.RepositoryBranchResult;
import com.aicoding.platform.orchestration.dto.RepositoryDiffSummaryResult;
import com.aicoding.platform.orchestration.dto.RepositoryFileSnippetResult;
import com.aicoding.platform.orchestration.dto.RepositoryReadFileItem;
import com.aicoding.platform.orchestration.dto.RepositoryTreeResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RepositoryReadToolService {

    private final ReadOnlyRepositoryAdapter repositoryAdapter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RepositoryReadToolService(ReadOnlyRepositoryAdapter repositoryAdapter) {
        this.repositoryAdapter = repositoryAdapter;
    }

    public static class RepositoryToolResult {
        private String summary;
        private String outputPayload;
        private List<String> filesRead;
        private String branch;
        private String pathPrefix;

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public String getOutputPayload() { return outputPayload; }
        public void setOutputPayload(String outputPayload) { this.outputPayload = outputPayload; }

        public List<String> getFilesRead() { return filesRead; }
        public void setFilesRead(List<String> filesRead) { this.filesRead = filesRead; }

        public String getBranch() { return branch; }
        public void setBranch(String branch) { this.branch = branch; }

        public String getPathPrefix() { return pathPrefix; }
        public void setPathPrefix(String pathPrefix) { this.pathPrefix = pathPrefix; }
    }

    /**
     * Execute a read-only repository tool.
     * Delegates to the ReadOnlyRepositoryAdapter for real filesystem reads.
     */
    public RepositoryToolResult executeReadOnlyTool(Long projectId, String toolKey,
                                                     Map<String, Object> parameters) {
        if (ToolName.READ_REPOSITORY_TREE.name().equals(toolKey)) {
            return executeReadRepositoryTree(parameters);
        } else if (ToolName.READ_FILE_SNIPPET.name().equals(toolKey)) {
            return executeReadFileSnippet(parameters);
        } else if (ToolName.READ_DIFF_SUMMARY.name().equals(toolKey)) {
            return executeReadDiffSummary(parameters);
        } else if (ToolName.READ_BRANCH_INFO.name().equals(toolKey)) {
            return executeReadBranchInfo(parameters);
        }
        throw new IllegalArgumentException("未知的仓库只读工具: " + toolKey);
    }

    private RepositoryToolResult executeReadRepositoryTree(Map<String, Object> parameters) {
        String branch = getStringParam(parameters, "branch", "main");
        String pathPrefix = getStringParam(parameters, "pathPrefix", "");
        int maxFiles = getIntParam(parameters, "maxFiles", 50);
        Long maxBytes = getLongParam(parameters, "maxBytes");

        ReadOnlyRepositoryRequest request = new ReadOnlyRepositoryRequest();
        request.setBranch(branch);
        request.setPathPrefix(pathPrefix);
        request.setMaxFiles(maxFiles);
        request.setMaxBytes(maxBytes);

        RepositoryTreeResult treeResult = repositoryAdapter.listTree(request);

        List<String> filesRead = treeResult.getFiles().stream()
                .map(RepositoryReadFileItem::getFilePath)
                .collect(Collectors.toList());

        int skippedCount = treeResult.getSkippedFiles() != null ? treeResult.getSkippedFiles().size() : 0;
        String summary = "已读取仓库文件树：分支=" + branch
                + (pathPrefix.isBlank() ? "" : ", 路径前缀=" + pathPrefix)
                + ", 文件数=" + filesRead.size()
                + (skippedCount > 0 ? ", 跳过=" + skippedCount : "")
                + (treeResult.isTruncated() ? ", 已截断" : "")
                + (treeResult.isRedacted() ? ", 已脱敏" : "");

        String outputPayload = buildTreeOutputPayload(treeResult, branch, pathPrefix, summary);

        RepositoryToolResult result = new RepositoryToolResult();
        result.setSummary(summary);
        result.setOutputPayload(outputPayload);
        result.setFilesRead(filesRead);
        result.setBranch(branch);
        result.setPathPrefix(pathPrefix);
        return result;
    }

    private RepositoryToolResult executeReadFileSnippet(Map<String, Object> parameters) {
        String branch = getStringParam(parameters, "branch", "main");
        String filePath = getStringParam(parameters, "filePath", "");
        int startLine = getIntParam(parameters, "startLine", 1);
        int maxLines = getIntParam(parameters, "maxLines", 80);
        Long maxBytes = getLongParam(parameters, "maxBytes");

        ReadOnlyRepositoryRequest request = new ReadOnlyRepositoryRequest();
        request.setBranch(branch);
        request.setFilePath(filePath);
        request.setStartLine(startLine);
        request.setMaxLines(maxLines);
        request.setMaxBytes(maxBytes);

        RepositoryFileSnippetResult snippetResult = repositoryAdapter.readSnippet(request);

        List<String> filesRead = snippetResult.getFilePath() != null && !snippetResult.getFilePath().isBlank()
                ? List.of(snippetResult.getFilePath()) : List.of();

        String contentSummary = snippetResult.getContent() != null
                ? snippetResult.getContent().length() + " chars" : "empty";
        String summary = "已读取文件片段：" + filePath
                + ", 行=" + snippetResult.getStartLine() + "-" + snippetResult.getEndLine()
                + ", 分支=" + branch
                + ", " + contentSummary
                + (snippetResult.isRedacted() ? ", 已脱敏(" + snippetResult.getRedactionCount() + ")" : "")
                + (snippetResult.isTruncated() ? ", 已截断" : "");

        String outputPayload = buildSnippetOutputPayload(snippetResult, branch, summary);

        RepositoryToolResult result = new RepositoryToolResult();
        result.setSummary(summary);
        result.setOutputPayload(outputPayload);
        result.setFilesRead(filesRead);
        result.setBranch(branch);
        result.setPathPrefix(filePath);
        return result;
    }

    private RepositoryToolResult executeReadDiffSummary(Map<String, Object> parameters) {
        String branch = getStringParam(parameters, "branch", "main");
        String baseBranch = getStringParam(parameters, "baseBranch", "main");
        int maxFiles = getIntParam(parameters, "maxFiles", 30);

        ReadOnlyRepositoryRequest request = new ReadOnlyRepositoryRequest();
        request.setBranch(branch);
        request.setBaseBranch(baseBranch);
        request.setMaxFiles(maxFiles);

        RepositoryDiffSummaryResult diffResult = repositoryAdapter.readDiffSummary(request);

        String summary = "已读取 Diff 摘要：" + baseBranch + "..." + branch
                + ", 最大文件数=" + maxFiles
                + (diffResult.isNoRealGitDiff() ? " (模拟数据，未执行实际 git diff)" : "");

        String outputPayload = buildDiffOutputPayload(diffResult, summary);

        RepositoryToolResult result = new RepositoryToolResult();
        result.setSummary(summary);
        result.setOutputPayload(outputPayload);
        result.setFilesRead(List.of());
        result.setBranch(branch);
        result.setPathPrefix(null);
        return result;
    }

    private RepositoryToolResult executeReadBranchInfo(Map<String, Object> parameters) {
        boolean includeRemote = getBooleanParam(parameters, "includeRemote", true);
        int maxBranches = getIntParam(parameters, "maxBranches", 30);

        ReadOnlyRepositoryRequest request = new ReadOnlyRepositoryRequest();
        request.setIncludeRemote(includeRemote);
        request.setMaxFiles(maxBranches);

        RepositoryBranchResult branchResult = repositoryAdapter.listBranches(request);

        List<String> branches = branchResult.getBranches();
        if (branches.size() > maxBranches) {
            branches = branches.subList(0, maxBranches);
        }

        String summary = "已读取分支信息：共 " + branches.size() + " 个分支"
                + (includeRemote ? "（含远程）" : "");

        String outputPayload = buildBranchOutputPayload(branchResult, branches, summary);

        RepositoryToolResult result = new RepositoryToolResult();
        result.setSummary(summary);
        result.setOutputPayload(outputPayload);
        result.setFilesRead(List.of());
        result.setBranch(null);
        result.setPathPrefix(null);
        return result;
    }

    // ========================
    // Output payload builders
    // ========================

    private String buildTreeOutputPayload(RepositoryTreeResult treeResult, String branch,
                                           String pathPrefix, String summary) {
        try {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("readOnly", true);
            map.put("toolKey", toolKey("READ_REPOSITORY_TREE"));
            if (branch != null && !branch.isBlank()) map.put("branch", branch);
            if (pathPrefix != null && !pathPrefix.isBlank()) map.put("pathPrefix", pathPrefix);
            map.put("filesRead", treeResult.getFiles().stream()
                    .map(RepositoryReadFileItem::getFilePath).collect(Collectors.toList()));
            map.put("skippedFiles", treeResult.getSkippedFiles() != null
                    ? treeResult.getSkippedFiles().stream()
                    .map(s -> Map.of("filePath", s.getFilePath(), "reason", s.getReason()))
                    .collect(Collectors.toList())
                    : List.of());
            map.put("filesTouched", List.of());
            map.put("gitOperations", List.of());
            map.put("redacted", treeResult.isRedacted());
            map.put("truncated", treeResult.isTruncated());
            map.put("summary", summary);
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{\"readOnly\":true,\"filesRead\":[],\"filesTouched\":[],\"gitOperations\":[],\"redacted\":false,\"truncated\":false}";
        }
    }

    private String buildSnippetOutputPayload(RepositoryFileSnippetResult snippetResult,
                                              String branch, String summary) {
        try {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("readOnly", true);
            map.put("toolKey", toolKey("READ_FILE_SNIPPET"));
            if (branch != null && !branch.isBlank()) map.put("branch", branch);
            map.put("filePath", snippetResult.getFilePath());
            map.put("snippet", snippetResult.getContent());
            map.put("startLine", snippetResult.getStartLine());
            map.put("endLine", snippetResult.getEndLine());
            map.put("totalLines", snippetResult.getTotalLines());
            map.put("language", snippetResult.getLanguage());
            map.put("filesRead", snippetResult.getFilePath() != null && !snippetResult.getFilePath().isBlank()
                    ? List.of(snippetResult.getFilePath()) : List.of());
            map.put("skippedFiles", snippetResult.getSkippedFiles() != null
                    ? snippetResult.getSkippedFiles().stream()
                    .map(s -> Map.of("filePath", s.getFilePath(), "reason", s.getReason()))
                    .collect(Collectors.toList())
                    : List.of());
            map.put("filesTouched", List.of());
            map.put("gitOperations", List.of());
            map.put("redacted", snippetResult.isRedacted());
            map.put("redactionCount", snippetResult.getRedactionCount());
            map.put("truncated", snippetResult.isTruncated());
            map.put("summary", summary);
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{\"readOnly\":true,\"filesRead\":[],\"filesTouched\":[],\"gitOperations\":[],\"redacted\":false,\"truncated\":false}";
        }
    }

    private String buildDiffOutputPayload(RepositoryDiffSummaryResult diffResult, String summary) {
        try {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("readOnly", true);
            map.put("toolKey", toolKey("READ_DIFF_SUMMARY"));
            map.put("baseBranch", diffResult.getBaseBranch());
            map.put("targetBranch", diffResult.getTargetBranch());
            map.put("fileCount", diffResult.getFileCount());
            map.put("additionCount", diffResult.getAdditionCount());
            map.put("deletionCount", diffResult.getDeletionCount());
            map.put("changedFiles", diffResult.getChangedFiles() != null ? diffResult.getChangedFiles() : List.of());
            map.put("filesRead", List.of());
            map.put("skippedFiles", List.of());
            map.put("filesTouched", List.of());
            map.put("gitOperations", List.of());
            map.put("noRealGitDiff", diffResult.isNoRealGitDiff());
            map.put("truncated", diffResult.isTruncated());
            map.put("redacted", false);
            map.put("summary", summary);
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{\"readOnly\":true,\"filesRead\":[],\"filesTouched\":[],\"gitOperations\":[],\"redacted\":false,\"truncated\":false}";
        }
    }

    private String buildBranchOutputPayload(RepositoryBranchResult branchResult,
                                             List<String> branches, String summary) {
        try {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("readOnly", true);
            map.put("toolKey", toolKey("READ_BRANCH_INFO"));
            map.put("branches", branches);
            map.put("includeRemote", branchResult.isIncludeRemote());
            map.put("noCheckout", branchResult.isNoCheckout());
            map.put("noPull", branchResult.isNoPull());
            map.put("filesRead", List.of());
            map.put("skippedFiles", List.of());
            map.put("filesTouched", List.of());
            map.put("gitOperations", List.of());
            map.put("redacted", false);
            map.put("truncated", false);
            map.put("summary", summary);
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{\"readOnly\":true,\"filesRead\":[],\"filesTouched\":[],\"gitOperations\":[],\"redacted\":false,\"truncated\":false}";
        }
    }

    private String toolKey(String methodName) {
        return getClass().getSimpleName() + "." + methodName;
    }

    private String getStringParam(Map<String, Object> params, String key, String defaultValue) {
        if (params != null && params.get(key) instanceof String s && !s.isBlank()) {
            return s;
        }
        return defaultValue;
    }

    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        if (params != null && params.get(key) instanceof Number n) {
            return n.intValue();
        }
        return defaultValue;
    }

    private Long getLongParam(Map<String, Object> params, String key) {
        if (params != null && params.get(key) instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    private boolean getBooleanParam(Map<String, Object> params, String key, boolean defaultValue) {
        if (params != null && params.get(key) instanceof Boolean b) {
            return b;
        }
        return defaultValue;
    }
}
