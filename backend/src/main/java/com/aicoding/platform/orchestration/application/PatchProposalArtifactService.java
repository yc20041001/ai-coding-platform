package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.ProjectToolConfigEntity;
import com.aicoding.platform.orchestration.domain.ToolCatalogEntity;
import com.aicoding.platform.orchestration.domain.ToolExecutionStatus;
import com.aicoding.platform.orchestration.domain.ToolName;
import com.aicoding.platform.orchestration.domain.ToolSandboxExecutionEntity;
import com.aicoding.platform.orchestration.infrastructure.ProjectToolConfigMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolCatalogMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolSandboxExecutionMapper;
import com.aicoding.platform.task.domain.AiTaskArtifactEntity;
import com.aicoding.platform.task.domain.AiTaskEntity;
import com.aicoding.platform.task.domain.TaskArtifactType;
import com.aicoding.platform.orchestration.domain.PatchProposalReviewEntity;
import com.aicoding.platform.orchestration.domain.PatchProposalReviewStatus;
import com.aicoding.platform.orchestration.infrastructure.PatchProposalReviewMapper;
import com.aicoding.platform.task.domain.AiTaskLogEntity;
import com.aicoding.platform.task.domain.TaskLogLevel;
import com.aicoding.platform.task.infrastructure.AiTaskArtifactMapper;
import com.aicoding.platform.task.infrastructure.AiTaskLogMapper;
import com.aicoding.platform.task.infrastructure.AiTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PatchProposalArtifactService {

    private final AiTaskArtifactMapper aiTaskArtifactMapper;
    private final AiTaskMapper aiTaskMapper;
    private final ToolParameterSchemaService toolParameterSchemaService;
    private final ToolCatalogMapper toolCatalogMapper;
    private final ProjectToolConfigMapper projectToolConfigMapper;
    private final ToolSandboxExecutionMapper toolSandboxExecutionMapper;
    private final PatchProposalReviewMapper patchProposalReviewMapper;
    private final AiTaskLogMapper aiTaskLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> REPOSITORY_TOOL_KEYS = Set.of(
            ToolName.READ_REPOSITORY_TREE.name(),
            ToolName.READ_FILE_SNIPPET.name(),
            ToolName.READ_DIFF_SUMMARY.name(),
            ToolName.READ_BRANCH_INFO.name()
    );

    private static final Set<String> CODE_SEARCH_TOOL_KEYS = Set.of(
            ToolName.READ_CODE_INDEX.name(),
            ToolName.SEARCH_CODE_SYMBOL.name(),
            ToolName.SEARCH_CODE_CHUNK.name()
    );

    public PatchProposalArtifactService(AiTaskArtifactMapper aiTaskArtifactMapper,
                                         AiTaskMapper aiTaskMapper,
                                         ToolParameterSchemaService toolParameterSchemaService,
                                         ToolCatalogMapper toolCatalogMapper,
                                         ProjectToolConfigMapper projectToolConfigMapper,
                                         ToolSandboxExecutionMapper toolSandboxExecutionMapper,
                                         PatchProposalReviewMapper patchProposalReviewMapper,
                                         AiTaskLogMapper aiTaskLogMapper) {
        this.aiTaskArtifactMapper = aiTaskArtifactMapper;
        this.aiTaskMapper = aiTaskMapper;
        this.toolParameterSchemaService = toolParameterSchemaService;
        this.toolCatalogMapper = toolCatalogMapper;
        this.projectToolConfigMapper = projectToolConfigMapper;
        this.toolSandboxExecutionMapper = toolSandboxExecutionMapper;
        this.patchProposalReviewMapper = patchProposalReviewMapper;
        this.aiTaskLogMapper = aiTaskLogMapper;
    }

    /**
     * Create a PATCH_PROPOSAL artifact for an approved MOCK_PATCH_PROPOSAL execution.
     * Returns the created artifact entity.
     */
    public AiTaskArtifactEntity createPatchProposalArtifact(ToolSandboxExecutionEntity execution) {
        // 1. Validate toolName = MOCK_PATCH_PROPOSAL
        if (!ToolName.MOCK_PATCH_PROPOSAL.name().equals(execution.getToolName())) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "仅 MOCK_PATCH_PROPOSAL 工具可以创建 Patch Proposal 产物，当前工具: " + execution.getToolName());
        }

        // 2. Validate execution.status = COMPLETED
        if (!ToolExecutionStatus.COMPLETED.name().equals(execution.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "仅 COMPLETED 状态的执行可以创建 Patch Proposal 产物，当前状态: " + execution.getStatus());
        }

        // 3. Check no duplicate artifact
        if (execution.getArtifactId() != null) {
            AiTaskArtifactEntity existing = aiTaskArtifactMapper.selectById(execution.getArtifactId());
            if (existing != null) {
                throw new BizException(ErrorCode.TOOL_APPROVAL_CONFLICT,
                        "该执行已存在 Patch Proposal 产物，不允许重复生成");
            }
        }

        // 4. Fetch task info for the title
        String taskTitle = "";
        if (execution.getTaskId() != null) {
            AiTaskEntity task = aiTaskMapper.selectById(execution.getTaskId());
            if (task != null) {
                taskTitle = task.getTitle();
            }
        }

        // 5. Resolve parameters for content customization
        Map<String, Object> parameters = resolveParameters(execution);

        // 6. Build markdown content with parameters and repository context
        String repoContextUsed = buildRepositoryContextUsed(execution);
        String codeSearchContext = buildCodeSearchContextUsed(execution);
        String content = buildPatchProposalContent(taskTitle, parameters, repoContextUsed, codeSearchContext);

        // 7. Insert artifact
        AiTaskArtifactEntity artifact = new AiTaskArtifactEntity();
        artifact.setTaskId(execution.getTaskId());
        artifact.setProjectId(execution.getProjectId());
        artifact.setArtifactType(TaskArtifactType.PATCH_PROPOSAL.name());
        artifact.setName("Mock Patch Proposal - " + (taskTitle.isEmpty() ? execution.getToolName() : taskTitle));
        artifact.setContent(content);
        aiTaskArtifactMapper.insert(artifact);

        // 8. Auto-create PENDING review
        createPendingReview(artifact, execution);

        return artifact;
    }

    /**
     * Query for repository tools executed within the same run to build a
     * "Repository Context Used" section.
     */
    private String buildRepositoryContextUsed(ToolSandboxExecutionEntity execution) {
        if (execution.getRunId() == null) return "";

        List<ToolSandboxExecutionEntity> runExecutions = toolSandboxExecutionMapper.selectList(
                new LambdaQueryWrapper<ToolSandboxExecutionEntity>()
                        .eq(ToolSandboxExecutionEntity::getRunId, execution.getRunId())
                        .in(ToolSandboxExecutionEntity::getToolName, REPOSITORY_TOOL_KEYS));

        if (runExecutions.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("\n## Repository Context Used\n\n");
        sb.append("> 只读上下文来源，未修改文件\n\n");

        for (ToolSandboxExecutionEntity repoExec : runExecutions) {
            sb.append("- **").append(repoExec.getToolName()).append("**");
            if (repoExec.getOutputPayload() != null && !repoExec.getOutputPayload().isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = objectMapper.readValue(
                            repoExec.getOutputPayload(), java.util.Map.class);
                    @SuppressWarnings("unchecked")
                    List<String> filesRead = payload.get("filesRead") instanceof List
                            ? (List<String>) payload.get("filesRead") : List.of();
                    if (!filesRead.isEmpty()) {
                        sb.append(": ").append(String.join(", ", filesRead));
                    }
                    // Mark redacted/truncated flags
                    boolean redacted = payload.get("redacted") instanceof Boolean
                            && Boolean.TRUE.equals(payload.get("redacted"));
                    boolean truncated = payload.get("truncated") instanceof Boolean
                            && Boolean.TRUE.equals(payload.get("truncated"));
                    if (redacted || truncated) {
                        sb.append(" (");
                        if (redacted) sb.append("已脱敏");
                        if (redacted && truncated) sb.append(", ");
                        if (truncated) sb.append("已截断");
                        sb.append(")");
                    }
                } catch (JsonProcessingException e) {
                    // ignore parse errors
                }
            }
            sb.append("\n");
        }

        sb.append("\n> 提示：Repository Context 为只读来源，已包含在上述分析中。\n");
        return sb.toString();
    }

    /**
     * Query for code search tools executed within the same run to build a
     * "Code Search Context Used" section.
     */
    private String buildCodeSearchContextUsed(ToolSandboxExecutionEntity execution) {
        if (execution.getRunId() == null) return "";

        List<ToolSandboxExecutionEntity> searchExecutions = toolSandboxExecutionMapper.selectList(
                new LambdaQueryWrapper<ToolSandboxExecutionEntity>()
                        .eq(ToolSandboxExecutionEntity::getRunId, execution.getRunId())
                        .in(ToolSandboxExecutionEntity::getToolName, CODE_SEARCH_TOOL_KEYS));

        if (searchExecutions.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("\n## Code Search Context Used\n\n");
        sb.append("> 代码搜索索引上下文，仅供搜索参考\n\n");

        for (ToolSandboxExecutionEntity searchExec : searchExecutions) {
            sb.append("- **").append(searchExec.getToolName()).append("**");
            if (searchExec.getOutputPayload() != null && !searchExec.getOutputPayload().isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = objectMapper.readValue(
                            searchExec.getOutputPayload(), java.util.Map.class);
                    Object matchedFiles = payload.get("matchedFiles");
                    Object totalCount = payload.get("totalCount");
                    if (matchedFiles instanceof Number) {
                        sb.append(": matchedFiles=").append(matchedFiles);
                    }
                    if (totalCount instanceof Number) {
                        sb.append(", totalCount=").append(totalCount);
                    }
                    Object keyword = payload.get("keyword");
                    if (keyword != null && !keyword.toString().isBlank()) {
                        sb.append(", keyword=").append(keyword);
                    }
                } catch (JsonProcessingException e) {
                    // ignore parse errors
                }
            }
            sb.append("\n");
        }

        sb.append("\n> 提示：Code Search Context 为代码索引搜索结果，仅包含符号和片段匹配。\n");
        return sb.toString();
    }

    private String buildPatchProposalContent(String taskTitle, Map<String, Object> parameters,
                                              String repoContextUsed, String codeSearchContext) {
        // Extract parameters with defaults
        String proposalScope = parameters != null && parameters.get("proposalScope") != null
                ? parameters.get("proposalScope").toString() : "MINIMAL";
        boolean includeTests = parameters != null && parameters.get("includeTests") instanceof Boolean
                ? Boolean.TRUE.equals(parameters.get("includeTests")) : true;
        int maxChangedFiles = parameters != null && parameters.get("maxChangedFiles") instanceof Number
                ? ((Number) parameters.get("maxChangedFiles")).intValue() : 3;
        String targetArea = parameters != null && parameters.get("targetArea") != null
                && !parameters.get("targetArea").toString().isBlank()
                ? parameters.get("targetArea").toString() : "未指定";

        // Advanced: targetFiles
        @SuppressWarnings("unchecked")
        List<String> targetFiles = parameters != null && parameters.get("targetFiles") instanceof List
                ? (List<String>) parameters.get("targetFiles") : List.of();

        // Advanced: testLevel
        String testLevel = parameters != null && parameters.get("testLevel") != null
                && !parameters.get("testLevel").toString().isBlank()
                ? parameters.get("testLevel").toString() : "INTEGRATION";

        String scopeLine = "- 提案范围: " + proposalScope + "\n";
        String targetLine = "- 目标区域: " + targetArea + "\n";
        String changesSummary = "- 最大变更文件数: " + maxChangedFiles + "\n";

        // Target files summary
        StringBuilder targetFilesSummary = new StringBuilder();
        if (!targetFiles.isEmpty()) {
            targetFilesSummary.append("- 目标文件:\n");
            for (String tf : targetFiles) {
                targetFilesSummary.append("  - ").append(tf).append("\n");
            }
        }

        // Build diff blocks — use targetFiles if available, else generate generic ones
        StringBuilder diffBlocks = new StringBuilder();
        int diffCount = Math.min(maxChangedFiles, targetFiles.isEmpty() ? maxChangedFiles : targetFiles.size());
        for (int i = 0; i < diffCount; i++) {
            String filePath = !targetFiles.isEmpty() && i < targetFiles.size()
                    ? targetFiles.get(i)
                    : i == 0 ? "src/main/java/com/example/Service.java"
                    : i == 1 ? "src/main/java/com/example/Controller.java"
                    : "src/main/java/com/example/Repository.java";
            diffBlocks.append("```diff\n")
                    .append("diff --git a/").append(filePath).append(" b/").append(filePath).append("\n")
                    .append("--- a/").append(filePath).append("\n")
                    .append("+++ b/").append(filePath).append("\n")
                    .append("@@ -1,5 +1,8 @@\n")
                    .append(" public class ").append(filePath.substring(filePath.lastIndexOf('/') + 1, filePath.lastIndexOf('.'))).append(" {\n")
                    .append("+    // Mock proposal only. Not applied.\n")
                    .append(" }\n")
                    .append("```\n\n");
        }

        // Build checklist — skip test items if includeTests is false
        StringBuilder checklist = new StringBuilder();
        checklist.append("- [ ] Confirm proposal matches task requirements\n");
        checklist.append("- [ ] Confirm no sensitive data is included\n");
        checklist.append("- [ ] Confirm no real file was modified\n");
        if (includeTests) {
            checklist.append("- [ ] Review test coverage for proposed changes\n");
            checklist.append("- [ ] Confirm tests pass in mock environment\n");
        }
        checklist.append("- [ ] Decide whether to manually implement in a future step\n");

        // Build test suggestion section
        StringBuilder testSection = new StringBuilder();
        if (includeTests) {
            testSection.append("\n## Test Suggestions\n\n");
            testSection.append("- 测试级别: ").append(testLevel).append("\n");
            testSection.append("- 建议编写对应的单元测试和集成测试\n");
            testSection.append("- 确保现有测试不受影响\n\n");
        }

        return "# Patch Proposal: " + (taskTitle.isEmpty() ? "Untitled Task" : taskTitle) + "\n"
                + "\n"
                + "> 安全提示：这是 Mock 补丁提案，仅用于审阅。系统没有写入文件，没有执行 git apply，没有提交代码。\n"
                + "\n"
                + "## Summary\n"
                + "\n"
                + "- Proposed change: 根据任务需求生成 Mock 补丁提案\n"
                + scopeLine
                + targetLine
                + changesSummary
                + targetFilesSummary.toString()
                + "- Generated by tool: MOCK_PATCH_PROPOSAL\n"
                + "- Execution mode: MOCK_EXECUTE\n"
                + "- Files touched: 0\n"
                + "- Git operations: 0\n"
                + "\n"
                + "## Proposed Diff (Mock)\n"
                + "\n"
                + diffBlocks.toString()
                + testSection.toString()
                + "## Review Checklist\n"
                + "\n"
                + checklist.toString()
                + (repoContextUsed != null && !repoContextUsed.isBlank() ? repoContextUsed : "")
                + (codeSearchContext != null && !codeSearchContext.isBlank() ? codeSearchContext : "")
                + "## Safety\n"
                + "\n"
                + "- mock: true\n"
                + "- filesTouched: []\n"
                + "- gitOperations: []\n"
                + "- applied: false\n";
    }

    /**
     * Auto-create a PENDING review for the generated PATCH_PROPOSAL artifact.
     */
    private void createPendingReview(AiTaskArtifactEntity artifact, ToolSandboxExecutionEntity execution) {
        PatchProposalReviewEntity review = new PatchProposalReviewEntity();
        review.setProjectId(artifact.getProjectId());
        review.setTaskId(artifact.getTaskId());
        review.setArtifactId(artifact.getId());
        review.setToolExecutionId(execution.getId());
        review.setStatus(PatchProposalReviewStatus.PENDING.name());
        review.setSafetyConfirmed(false);
        review.setChecklistJson("{\"matchesRequirement\":false,\"noSensitiveData\":false,\"noFileWritten\":true,\"noGitOperation\":true,\"readyForManualImplementation\":false}");
        patchProposalReviewMapper.insert(review);

        AiTaskLogEntity taskLog = new AiTaskLogEntity();
        taskLog.setTaskId(artifact.getTaskId());
        taskLog.setProjectId(artifact.getProjectId());
        taskLog.setLevel(TaskLogLevel.INFO.name());
        taskLog.setStage("PATCH_PROPOSAL_REVIEW_CREATED");
        taskLog.setMessage("Patch Proposal 审阅记录已创建，等待人工审阅。");
        aiTaskLogMapper.insert(taskLog);
    }

    private Map<String, Object> resolveParameters(ToolSandboxExecutionEntity execution) {
        ToolCatalogEntity tool = toolCatalogMapper.selectOne(
                new LambdaQueryWrapper<ToolCatalogEntity>()
                        .eq(ToolCatalogEntity::getToolKey, ToolName.MOCK_PATCH_PROPOSAL.name()));
        if (tool == null) {
            return Map.of();
        }

        ProjectToolConfigEntity config = projectToolConfigMapper.selectOne(
                new LambdaQueryWrapper<ProjectToolConfigEntity>()
                        .eq(ProjectToolConfigEntity::getProjectId, execution.getProjectId())
                        .eq(ProjectToolConfigEntity::getToolId, tool.getId()));

        String parametersJson = config != null ? config.getParametersJson() : null;
        Map<String, Object> rawParameters = new java.util.HashMap<>();
        if (parametersJson != null && !parametersJson.isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = objectMapper.readValue(parametersJson, java.util.Map.class);
                if (parsed != null) {
                    rawParameters = parsed;
                }
            } catch (JsonProcessingException e) {
                // ignore
            }
        }

        return toolParameterSchemaService.normalizeAndValidate(
                tool.getParameterSchemaJson(), rawParameters);
    }
}
