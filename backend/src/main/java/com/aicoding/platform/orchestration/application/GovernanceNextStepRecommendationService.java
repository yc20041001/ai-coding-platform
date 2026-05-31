package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.*;
import com.aicoding.platform.orchestration.dto.GovernanceNextStepRecommendationResponse;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.infrastructure.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceNextStepRecommendationService {

    private final GovernanceNextStepRecommendationMapper nextStepMapper;
    private final GovernanceRecommendationWorkflowService workflowService;
    private final GovernanceWaiverRequestMapper waiverMapper;

    public GovernanceNextStepRecommendationService(GovernanceNextStepRecommendationMapper nextStepMapper,
                                                    GovernanceRecommendationWorkflowService workflowService,
                                                    GovernanceWaiverRequestMapper waiverMapper) {
        this.nextStepMapper = nextStepMapper;
        this.workflowService = workflowService;
        this.waiverMapper = waiverMapper;
    }

    @Transactional
    public List<GovernanceNextStepRecommendationResponse> refreshNextSteps(String sessionIdStr) {
        Long sessionId = parseLong(sessionIdStr);
        LambdaQueryWrapper<GovernanceNextStepRecommendationEntity> delW = new LambdaQueryWrapper<>();
        delW.eq(GovernanceNextStepRecommendationEntity::getSessionId, sessionId);
        nextStepMapper.delete(delW);

        List<GovernanceNextStepRecommendationEntity> steps = new ArrayList<>();
        int rank = 0;

        List<GovernanceRecommendationItemEntity> items = workflowService.getOpenItems();

        // Check for active waivers
        long activeWaivers = waiverMapper.selectCount(
                new LambdaQueryWrapper<GovernanceWaiverRequestEntity>()
                        .eq(GovernanceWaiverRequestEntity::getWaiverStatus, "APPROVED"));

        // 1. High priority without execution plan → OPEN_PLAYBOOK
        for (var item : items) {
            if ("P0".equals(item.getPriority()) || "P1".equals(item.getPriority())) {
                if (rank >= 5) break;
                rank++;
                GovernanceNextStepRecommendationEntity s = new GovernanceNextStepRecommendationEntity();
                s.setSessionId(sessionId); s.setRecommendationId(item.getId()); s.setSuggestionRank(rank);
                s.setSuggestionType("OPEN_PLAYBOOK");
                s.setTitle("Open playbook for high-priority recommendation");
                s.setSummaryText("Recommendation: " + item.getTitle());
                s.setRationaleText("High priority recommendation - start with playbook execution");
                s.setExpectedOutcomeText("Structured execution path for priority item");
                steps.add(s);
            }
        }

        // 2. Active waiver → REVIEW_WAIVER
        if (activeWaivers > 0 && rank < 5) {
            rank++;
            GovernanceNextStepRecommendationEntity s = new GovernanceNextStepRecommendationEntity();
            s.setSessionId(sessionId); s.setSuggestionRank(rank);
            s.setSuggestionType("REVIEW_WAIVER");
            s.setTitle("Review " + activeWaivers + " active waiver(s)");
            s.setRationaleText("Active waivers need attention before they expire");
            s.setExpectedOutcomeText("Reduced waiver expiry risk");
            steps.add(s);
        }

        // 3. Blocked items → START_HANDOFF
        long blockedCount = items.stream().filter(i -> "BLOCKED".equals(i.getWorkflowStatus())).count();
        if (blockedCount > 0 && rank < 5) {
            rank++;
            GovernanceNextStepRecommendationEntity s = new GovernanceNextStepRecommendationEntity();
            s.setSessionId(sessionId); s.setSuggestionRank(rank);
            s.setSuggestionType("START_HANDOFF");
            s.setTitle("Handle " + blockedCount + " blocked recommendation(s)");
            s.setRationaleText("Blocked items may need owner reassignment or escalation");
            s.setExpectedOutcomeText("Unblocked recommendations");
            steps.add(s);
        }

        // 4. Overdue items → REVIEW_FORECAST
        long overdueCount = items.stream().filter(i -> i.getDueAt() != null && i.getDueAt().isBefore(LocalDateTime.now())).count();
        if (overdueCount > 0 && rank < 5) {
            rank++;
            GovernanceNextStepRecommendationEntity s = new GovernanceNextStepRecommendationEntity();
            s.setSessionId(sessionId); s.setSuggestionRank(rank);
            s.setSuggestionType("REVIEW_FORECAST");
            s.setTitle("Review forecast for " + overdueCount + " overdue item(s)");
            s.setRationaleText("Overdue trend indicates growing backlog risk");
            s.setExpectedOutcomeText("Better visibility into overdue impact");
            steps.add(s);
        }

        // 5. Knowledge suggestion
        if (rank < 5) {
            rank++;
            GovernanceNextStepRecommendationEntity s = new GovernanceNextStepRecommendationEntity();
            s.setSessionId(sessionId); s.setSuggestionRank(rank);
            s.setSuggestionType("OPEN_KNOWLEDGE");
            s.setTitle("Browse knowledge base for similar cases");
            s.setRationaleText("Knowledge base contains reusable remediation patterns");
            s.setExpectedOutcomeText("Faster resolution through reuse");
            steps.add(s);
        }

        for (var s : steps) nextStepMapper.insert(s);
        return getNextSteps(sessionIdStr);
    }

    @Transactional(readOnly = true)
    public List<GovernanceNextStepRecommendationResponse> getNextSteps(String sessionIdStr) {
        Long sessionId = parseLong(sessionIdStr);
        LambdaQueryWrapper<GovernanceNextStepRecommendationEntity> w = new LambdaQueryWrapper<>();
        w.eq(GovernanceNextStepRecommendationEntity::getSessionId, sessionId);
        w.orderByAsc(GovernanceNextStepRecommendationEntity::getSuggestionRank);
        return nextStepMapper.selectList(w).stream().map(this::toResponse).collect(Collectors.toList());
    }

    private GovernanceNextStepRecommendationResponse toResponse(GovernanceNextStepRecommendationEntity e) {
        GovernanceNextStepRecommendationResponse r = new GovernanceNextStepRecommendationResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSessionId(e.getSessionId() != null ? e.getSessionId().toString() : null);
        r.setGuidedTaskId(e.getGuidedTaskId() != null ? e.getGuidedTaskId().toString() : null);
        r.setRecommendationId(e.getRecommendationId() != null ? e.getRecommendationId().toString() : null);
        r.setSuggestionRank(e.getSuggestionRank()); r.setSuggestionType(e.getSuggestionType());
        r.setTitle(e.getTitle()); r.setSummaryText(e.getSummaryText()); r.setRationaleText(e.getRationaleText());
        r.setExpectedOutcomeText(e.getExpectedOutcomeText()); r.setActionPayloadJson(e.getActionPayloadJson());
        return r;
    }

    private static Long parseLong(String v) { try { return Long.valueOf(v); } catch (NumberFormatException e) { throw new BizException(ErrorCode.BAD_REQUEST, "ID 格式无效"); } }
}
