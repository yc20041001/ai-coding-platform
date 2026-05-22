package com.aicoding.platform.orchestration.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.aicoding.platform.orchestration.domain.WorkflowTemplateEntity;
import com.aicoding.platform.orchestration.dto.WorkflowPhaseTemplateResponse;
import com.aicoding.platform.orchestration.dto.WorkflowStepTemplateResponse;
import com.aicoding.platform.orchestration.dto.WorkflowStrategyResponse;
import com.aicoding.platform.orchestration.infrastructure.WorkflowTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class WorkflowStrategyCatalogService {

    private final WorkflowTemplateMapper workflowTemplateMapper;

    public WorkflowStrategyCatalogService(WorkflowTemplateMapper workflowTemplateMapper) {
        this.workflowTemplateMapper = workflowTemplateMapper;
    }

    // ========================
    // Internal template model
    // ========================

    public static class StrategyTemplate {
        private final String strategyKey;
        private final String name;
        private final String description;
        private final List<PhaseTemplate> phases;
        private final List<ApprovalGateTemplate> approvalGates;

        public StrategyTemplate(String strategyKey, String name, String description,
                                List<PhaseTemplate> phases, List<ApprovalGateTemplate> approvalGates) {
            this.strategyKey = strategyKey;
            this.name = name;
            this.description = description;
            this.phases = phases;
            this.approvalGates = approvalGates != null ? approvalGates : List.of();
        }

        public String getStrategyKey() { return strategyKey; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<PhaseTemplate> getPhases() { return phases; }
        public List<ApprovalGateTemplate> getApprovalGates() { return approvalGates; }
    }

    public static class PhaseTemplate {
        private final int phaseOrder;
        private final String phaseKey;
        private final String title;
        private final List<StepTemplate> steps;

        public PhaseTemplate(int phaseOrder, String phaseKey, String title, List<StepTemplate> steps) {
            this.phaseOrder = phaseOrder;
            this.phaseKey = phaseKey;
            this.title = title;
            this.steps = steps;
        }

        public int getPhaseOrder() { return phaseOrder; }
        public String getPhaseKey() { return phaseKey; }
        public String getTitle() { return title; }
        public List<StepTemplate> getSteps() { return steps; }
    }

    public static class StepTemplate {
        private final String stepType;
        private final String agentCode;
        private final String laneKey;
        private final String title;

        public StepTemplate(String stepType, String agentCode, String laneKey, String title) {
            this.stepType = stepType;
            this.agentCode = agentCode;
            this.laneKey = laneKey;
            this.title = title;
        }

        public String getStepType() { return stepType; }
        public String getAgentCode() { return agentCode; }
        public String getLaneKey() { return laneKey; }
        public String getTitle() { return title; }
    }

    public static class ApprovalGateTemplate {
        private final String gateKey;
        private final String title;
        private final String description;
        private final int afterPhaseOrder;

        public ApprovalGateTemplate(String gateKey, String title, String description, int afterPhaseOrder) {
            this.gateKey = gateKey;
            this.title = title;
            this.description = description;
            this.afterPhaseOrder = afterPhaseOrder;
        }

        public String getGateKey() { return gateKey; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public int getAfterPhaseOrder() { return afterPhaseOrder; }
    }

    // ========================
    // Built-in strategies (fallback)
    // ========================

    private static final List<StrategyTemplate> BUILTIN_STRATEGIES = List.of(
            new StrategyTemplate("STANDARD_DELIVERY", "标准交付流程",
                    "架构 → 后端/前端/测试并行 → 审查 → 总结",
                    List.of(
                            new PhaseTemplate(1, "PLANNING", "架构规划", List.of(
                                    new StepTemplate("ARCHITECTURE_ANALYSIS", "architect-agent", "architect", "架构分析")
                            )),
                            new PhaseTemplate(2, "IMPLEMENTATION", "实现方案并行分析", List.of(
                                    new StepTemplate("BACKEND_IMPLEMENTATION_PLAN", "backend-agent", "backend", "后端实现计划"),
                                    new StepTemplate("FRONTEND_IMPLEMENTATION_PLAN", "frontend-agent", "frontend", "前端实现计划"),
                                    new StepTemplate("TEST_PLAN", "test-agent", "test", "测试计划")
                            )),
                            new PhaseTemplate(3, "REVIEW", "综合审查", List.of(
                                    new StepTemplate("CODE_REVIEW", "review-agent", "review", "代码审查")
                            )),
                            new PhaseTemplate(4, "SUMMARY", "最终总结", List.of(
                                    new StepTemplate("FINAL_SUMMARY", "architect-agent", "summary", "最终总结")
                            ))
                    ),
                    List.of(
                            new ApprovalGateTemplate("IMPLEMENTATION_PLAN_APPROVAL", "实施方案审批",
                                    "请确认多智能体生成的实施方案是否可以进入审查与总结阶段。", 2)
                    )),
            new StrategyTemplate("BACKEND_FOCUSED", "后端优先流程",
                    "架构 → 后端/测试并行 → 审查 → 总结",
                    List.of(
                            new PhaseTemplate(1, "PLANNING", "架构规划", List.of(
                                    new StepTemplate("ARCHITECTURE_ANALYSIS", "architect-agent", "architect", "架构分析")
                            )),
                            new PhaseTemplate(2, "BACKEND_IMPLEMENTATION", "后端实现分析", List.of(
                                    new StepTemplate("BACKEND_IMPLEMENTATION_PLAN", "backend-agent", "backend", "后端实现计划"),
                                    new StepTemplate("TEST_PLAN", "test-agent", "test", "测试计划")
                            )),
                            new PhaseTemplate(3, "REVIEW", "综合审查", List.of(
                                    new StepTemplate("CODE_REVIEW", "review-agent", "review", "代码审查")
                            )),
                            new PhaseTemplate(4, "SUMMARY", "最终总结", List.of(
                                    new StepTemplate("FINAL_SUMMARY", "architect-agent", "summary", "最终总结")
                            ))
                    ),
                    List.of(
                            new ApprovalGateTemplate("IMPLEMENTATION_PLAN_APPROVAL", "实施方案审批",
                                    "请确认多智能体生成的后端实施方案是否可以进入审查与总结阶段。", 2)
                    )),
            new StrategyTemplate("FRONTEND_FOCUSED", "前端优先流程",
                    "架构 → 前端/测试并行 → 审查 → 总结",
                    List.of(
                            new PhaseTemplate(1, "PLANNING", "架构规划", List.of(
                                    new StepTemplate("ARCHITECTURE_ANALYSIS", "architect-agent", "architect", "架构分析")
                            )),
                            new PhaseTemplate(2, "FRONTEND_IMPLEMENTATION", "前端实现分析", List.of(
                                    new StepTemplate("FRONTEND_IMPLEMENTATION_PLAN", "frontend-agent", "frontend", "前端实现计划"),
                                    new StepTemplate("TEST_PLAN", "test-agent", "test", "测试计划")
                            )),
                            new PhaseTemplate(3, "REVIEW", "综合审查", List.of(
                                    new StepTemplate("CODE_REVIEW", "review-agent", "review", "代码审查")
                            )),
                            new PhaseTemplate(4, "SUMMARY", "最终总结", List.of(
                                    new StepTemplate("FINAL_SUMMARY", "architect-agent", "summary", "最终总结")
                            ))
                    ),
                    List.of(
                            new ApprovalGateTemplate("IMPLEMENTATION_PLAN_APPROVAL", "实施方案审批",
                                    "请确认多智能体生成的前端实施方案是否可以进入审查与总结阶段。", 2)
                    )),
            new StrategyTemplate("REVIEW_ONLY", "审查流程",
                    "审查 → 总结",
                    List.of(
                            new PhaseTemplate(1, "REVIEW", "综合审查", List.of(
                                    new StepTemplate("CODE_REVIEW", "review-agent", "review", "代码审查")
                            )),
                            new PhaseTemplate(2, "SUMMARY", "最终总结", List.of(
                                    new StepTemplate("FINAL_SUMMARY", "architect-agent", "summary", "最终总结")
                            ))
                    ),
                    List.of())
    );

    private static final Map<String, String> LEGACY_MAPPING = new LinkedHashMap<>();

    static {
        LEGACY_MAPPING.put("DEFAULT_MOCK", "STANDARD_DELIVERY");
        LEGACY_MAPPING.put("PHASED_PARALLEL_MOCK", "STANDARD_DELIVERY");
    }

    private static final Set<String> VALID_KEYS = Set.of(
            "STANDARD_DELIVERY", "BACKEND_FOCUSED", "FRONTEND_FOCUSED", "REVIEW_ONLY"
    );

    // ========================
    // Public API
    // ========================

    public List<WorkflowStrategyResponse> listStrategies() {
        // Try DB first — return only ENABLED templates
        List<WorkflowTemplateEntity> dbTemplates = queryEnabledTemplates();
        if (!dbTemplates.isEmpty()) {
            List<WorkflowStrategyResponse> result = new ArrayList<>();
            for (WorkflowTemplateEntity entity : dbTemplates) {
                try {
                    WorkflowStrategyResponse resp = toResponse(entity);
                    result.add(resp);
                } catch (Exception e) {
                    // Skip templates with broken JSON
                }
            }
            return result;
        }

        // Fallback to built-in templates (no DB data)
        List<WorkflowStrategyResponse> result = new ArrayList<>();
        for (StrategyTemplate st : BUILTIN_STRATEGIES) {
            result.add(toResponse(st));
        }
        return result;
    }

    public StrategyTemplate resolveTemplate(String strategyKey) {
        String normalized = normalizeStrategyKey(strategyKey);

        // Try DB first — only ENABLED templates
        List<WorkflowTemplateEntity> dbTemplates = queryEnabledTemplates();
        for (WorkflowTemplateEntity entity : dbTemplates) {
            if (normalized.equals(entity.getTemplateKey())) {
                try {
                    return toStrategyTemplate(entity);
                } catch (Exception e) {
                    throw new com.aicoding.platform.common.exception.BizException(
                            com.aicoding.platform.common.exception.ErrorCode.INTERNAL_ERROR,
                            "模板JSON解析失败 (" + entity.getTemplateKey() + "): " + e.getMessage());
                }
            }
        }

        // Try DB (even non-empty result may not contain the target key)
        // Fallback to built-in
        for (StrategyTemplate st : BUILTIN_STRATEGIES) {
            if (st.getStrategyKey().equals(normalized)) {
                return st;
            }
        }

        throw new com.aicoding.platform.common.exception.BizException(
                com.aicoding.platform.common.exception.ErrorCode.BAD_REQUEST,
                "未知的 strategy: " + normalized);
    }

    public String normalizeStrategyKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "STANDARD_DELIVERY";
        }
        String upper = raw.trim().toUpperCase();
        String mapped = LEGACY_MAPPING.get(upper);
        if (mapped != null) {
            return mapped;
        }
        return upper;
    }

    public boolean isValidStrategy(String strategyKey) {
        if (strategyKey == null || strategyKey.isBlank()) {
            return true; // default
        }
        String upper = strategyKey.trim().toUpperCase();
        if (LEGACY_MAPPING.containsKey(upper)) {
            return true;
        }

        // Check DB enabled templates
        List<WorkflowTemplateEntity> dbTemplates = queryEnabledTemplates();
        if (!dbTemplates.isEmpty()) {
            for (WorkflowTemplateEntity entity : dbTemplates) {
                if (upper.equals(entity.getTemplateKey())) {
                    return true;
                }
            }
            return false; // DB has templates but this key is not enabled
        }

        // Fallback to built-in validation
        return VALID_KEYS.contains(upper);
    }

    // ========================
    // DB integration
    // ========================

    private List<WorkflowTemplateEntity> queryEnabledTemplates() {
        LambdaQueryWrapper<WorkflowTemplateEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowTemplateEntity::getStatus, "ENABLED");
        wrapper.orderByAsc(WorkflowTemplateEntity::getId);
        return workflowTemplateMapper.selectList(wrapper);
    }

    private WorkflowStrategyResponse toResponse(WorkflowTemplateEntity entity) {
        WorkflowStrategyResponse resp = new WorkflowStrategyResponse();
        resp.setStrategyKey(entity.getTemplateKey());
        resp.setName(entity.getName());
        resp.setDescription(entity.getDescription());

        // Parse phase/step counts from template_json
        List<WorkflowPhaseTemplateResponse> phaseResps = new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = om.readTree(entity.getTemplateJson());
            com.fasterxml.jackson.databind.JsonNode phasesNode = root.get("phases");
            int pc = 0;
            int sc = 0;
            if (phasesNode != null && phasesNode.isArray()) {
                pc = phasesNode.size();
                for (com.fasterxml.jackson.databind.JsonNode p : phasesNode) {
                    com.fasterxml.jackson.databind.JsonNode stepsNode = p.get("steps");
                    WorkflowPhaseTemplateResponse pr = new WorkflowPhaseTemplateResponse();
                    pr.setPhaseOrder(p.get("phaseOrder").asInt());
                    pr.setPhaseKey(pathStr(p, "phaseKey"));
                    pr.setTitle(pathStr(p, "title"));

                    List<WorkflowStepTemplateResponse> stepResps = new ArrayList<>();
                    int stepOrder = 1;
                    if (stepsNode != null && stepsNode.isArray()) {
                        sc += stepsNode.size();
                        for (com.fasterxml.jackson.databind.JsonNode sn : stepsNode) {
                            WorkflowStepTemplateResponse sr = new WorkflowStepTemplateResponse();
                            sr.setStepOrder(stepOrder++);
                            sr.setStepType(pathStr(sn, "stepType"));
                            sr.setAgentCode(pathStr(sn, "agentCode"));
                            sr.setLaneKey(pathStr(sn, "laneKey"));
                            sr.setTitle(pathStr(sn, "title"));
                            stepResps.add(sr);
                        }
                    }
                    pr.setSteps(stepResps);
                    phaseResps.add(pr);
                }
            }
            resp.setPhaseCount(pc);
            resp.setStepCount(sc);
        } catch (JsonProcessingException e) {
            resp.setPhaseCount(0);
            resp.setStepCount(0);
        }

        resp.setPhases(phaseResps);
        return resp;
    }

    private StrategyTemplate toStrategyTemplate(WorkflowTemplateEntity entity) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = om.readTree(entity.getTemplateJson());

            List<PhaseTemplate> phases = new ArrayList<>();
            com.fasterxml.jackson.databind.JsonNode phasesNode = root.get("phases");
            if (phasesNode != null && phasesNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode pn : phasesNode) {
                    int phaseOrder = pn.get("phaseOrder").asInt();
                    String phaseKey = pathStr(pn, "phaseKey");
                    String title = pathStr(pn, "title");
                    List<StepTemplate> steps = new ArrayList<>();
                    com.fasterxml.jackson.databind.JsonNode stepsNode = pn.get("steps");
                    if (stepsNode != null && stepsNode.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode sn : stepsNode) {
                            steps.add(new StepTemplate(
                                    pathStr(sn, "stepType"),
                                    pathStr(sn, "agentCode"),
                                    pathStr(sn, "laneKey"),
                                    pathStr(sn, "title")
                            ));
                        }
                    }
                    phases.add(new PhaseTemplate(phaseOrder, phaseKey, title, steps));
                }
            }

            List<ApprovalGateTemplate> gates = new ArrayList<>();
            com.fasterxml.jackson.databind.JsonNode gatesNode = root.get("approvalGates");
            if (gatesNode != null && gatesNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode gn : gatesNode) {
                    gates.add(new ApprovalGateTemplate(
                            pathStr(gn, "gateKey"),
                            pathStr(gn, "title"),
                            pathStr(gn, "description"),
                            gn.get("afterPhaseOrder").asInt()
                    ));
                }
            }

            return new StrategyTemplate(
                    entity.getTemplateKey(),
                    entity.getName(),
                    entity.getDescription(),
                    phases,
                    gates
            );
        } catch (JsonProcessingException e) {
            throw new com.aicoding.platform.common.exception.BizException(
                    com.aicoding.platform.common.exception.ErrorCode.INTERNAL_ERROR,
                    "模板JSON解析失败: " + e.getMessage());
        }
    }

    private String pathStr(com.fasterxml.jackson.databind.JsonNode node, String field) {
        com.fasterxml.jackson.databind.JsonNode child = node.get(field);
        return child != null && !child.isNull() ? child.asText() : "";
    }

    // ========================
    // DTO mapping
    // ========================

    private WorkflowStrategyResponse toResponse(StrategyTemplate st) {
        WorkflowStrategyResponse resp = new WorkflowStrategyResponse();
        resp.setStrategyKey(st.getStrategyKey());
        resp.setName(st.getName());
        resp.setDescription(st.getDescription());
        resp.setPhaseCount(st.getPhases().size());

        int stepCount = 0;
        List<WorkflowPhaseTemplateResponse> phaseResps = new ArrayList<>();
        for (PhaseTemplate pt : st.getPhases()) {
            WorkflowPhaseTemplateResponse pr = new WorkflowPhaseTemplateResponse();
            pr.setPhaseOrder(pt.getPhaseOrder());
            pr.setPhaseKey(pt.getPhaseKey());
            pr.setTitle(pt.getTitle());

            int stepOrder = 1;
            List<WorkflowStepTemplateResponse> stepResps = new ArrayList<>();
            for (StepTemplate s : pt.getSteps()) {
                WorkflowStepTemplateResponse sr = new WorkflowStepTemplateResponse();
                sr.setStepOrder(stepOrder++);
                sr.setStepType(s.getStepType());
                sr.setAgentCode(s.getAgentCode());
                sr.setLaneKey(s.getLaneKey());
                sr.setTitle(s.getTitle());
                stepResps.add(sr);
            }
            pr.setSteps(stepResps);
            phaseResps.add(pr);
            stepCount += pt.getSteps().size();
        }
        resp.setStepCount(stepCount);
        resp.setPhases(phaseResps);
        return resp;
    }
}
