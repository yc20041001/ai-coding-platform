package com.aicoding.platform.github.application;

import com.aicoding.platform.audit.application.AuditLogApplicationService;
import com.aicoding.platform.audit.domain.AuditActionType;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.github.domain.GithubPullRequestCacheEntity;
import com.aicoding.platform.github.dto.GithubPullRequestFileResponse;
import com.aicoding.platform.github.dto.GithubPullRequestResponse;
import com.aicoding.platform.github.infrastructure.GithubPullRequestCacheMapper;
import com.aicoding.platform.security.context.LoginUserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GithubPullRequestService {

    private final GithubClient githubClient;
    private final GithubOAuthService githubOAuthService;
    private final GithubPullRequestCacheMapper prCacheMapper;
    private final AuditLogApplicationService auditLogApplicationService;

    public GithubPullRequestService(GithubClient githubClient,
                                    GithubOAuthService githubOAuthService,
                                    GithubPullRequestCacheMapper prCacheMapper,
                                    AuditLogApplicationService auditLogApplicationService) {
        this.githubClient = githubClient;
        this.githubOAuthService = githubOAuthService;
        this.prCacheMapper = prCacheMapper;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional
    public List<GithubPullRequestResponse> listPullRequests(String owner, String repo, String state) {
        Long userId = LoginUserContext.currentUserId();
        if (userId == null) throw new BizException(ErrorCode.UNAUTHORIZED);

        String token = githubOAuthService.getAccessToken(userId);
        List<JsonNode> prs = githubClient.listPullRequests(token, owner, repo, state);

        List<GithubPullRequestResponse> result = new ArrayList<>();
        for (JsonNode pr : prs) {
            GithubPullRequestCacheEntity cached = cachePr(pr, null);
            result.add(toResponse(cached));
        }

        auditLogApplicationService.recordSuccess(null, null,
                AuditActionType.GITHUB_PR_FETCH.name(), "GITHUB_PR",
                owner + "/" + repo + " PRs: " + result.size());

        return result;
    }

    public GithubPullRequestResponse getDetail(String owner, String repo, int number) {
        Long userId = LoginUserContext.currentUserId();
        if (userId == null) throw new BizException(ErrorCode.UNAUTHORIZED);

        String token = githubOAuthService.getAccessToken(userId);
        JsonNode pr = githubClient.getPullRequest(token, owner, repo, number);
        GithubPullRequestCacheEntity cached = cachePr(pr, null);
        return toResponse(cached);
    }

    public List<GithubPullRequestFileResponse> getFiles(String owner, String repo, int number) {
        Long userId = LoginUserContext.currentUserId();
        if (userId == null) throw new BizException(ErrorCode.UNAUTHORIZED);

        String token = githubOAuthService.getAccessToken(userId);
        List<JsonNode> files = githubClient.listPullRequestFiles(token, owner, repo, number);

        List<GithubPullRequestFileResponse> result = new ArrayList<>();
        for (JsonNode f : files) {
            GithubPullRequestFileResponse resp = new GithubPullRequestFileResponse();
            resp.setFilename(f.has("filename") ? f.get("filename").asText() : "");
            resp.setStatus(f.has("status") ? f.get("status").asText() : "");
            resp.setAdditions(f.has("additions") ? f.get("additions").asInt() : 0);
            resp.setDeletions(f.has("deletions") ? f.get("deletions").asInt() : 0);
            resp.setChanges(f.has("changes") ? f.get("changes").asInt() : 0);
            resp.setPatch(f.has("patch") ? f.get("patch").asText() : null);
            result.add(resp);
        }
        return result;
    }

    public String getPatch(String owner, String repo, int number) {
        Long userId = LoginUserContext.currentUserId();
        if (userId == null) throw new BizException(ErrorCode.UNAUTHORIZED);

        String token = githubOAuthService.getAccessToken(userId);

        JsonNode pr = githubClient.getPullRequest(token, owner, repo, number);
        String diffUrl = pr.has("diff_url") ? pr.get("diff_url").asText() : null;

        if (diffUrl == null || diffUrl.isBlank()) {
            throw new BizException(ErrorCode.NOT_FOUND, "PR diff URL 不可用");
        }

        String patch = githubClient.getPullRequestPatch(token, diffUrl);

        int maxChars = 50000;
        if (patch.length() > maxChars) {
            patch = patch.substring(0, maxChars) + "\n\n... [truncated, " + (patch.length() - maxChars) + " more chars]";
        }
        return patch;
    }

    private GithubPullRequestCacheEntity cachePr(JsonNode pr, Long projectId) {
        Long githubRepoId = pr.has("base") && pr.get("base").has("repo") && pr.get("base").get("repo").has("id")
                ? pr.get("base").get("repo").get("id").asLong() : 0L;
        int number = pr.has("number") ? pr.get("number").asInt() : 0;

        GithubPullRequestCacheEntity existing = prCacheMapper.selectOne(
                new LambdaQueryWrapper<GithubPullRequestCacheEntity>()
                        .eq(GithubPullRequestCacheEntity::getGithubRepoId, githubRepoId)
                        .eq(GithubPullRequestCacheEntity::getNumber, number));

        GithubPullRequestCacheEntity entity = existing != null ? existing : new GithubPullRequestCacheEntity();
        entity.setProjectId(projectId);
        entity.setGithubPrId(pr.has("id") ? pr.get("id").asLong() : 0L);
        entity.setGithubRepoId(githubRepoId);
        entity.setNumber(number);
        entity.setTitle(pr.has("title") ? pr.get("title").asText() : "");
        entity.setState(pr.has("state") ? pr.get("state").asText().toUpperCase() : "OPEN");
        entity.setAuthorLogin(pr.has("user") && pr.get("user").has("login")
                ? pr.get("user").get("login").asText() : null);
        entity.setBaseBranch(pr.has("base") && pr.get("base").has("ref")
                ? pr.get("base").get("ref").asText() : null);
        entity.setHeadBranch(pr.has("head") && pr.get("head").has("ref")
                ? pr.get("head").get("ref").asText() : null);
        entity.setHtmlUrl(pr.has("html_url") ? pr.get("html_url").asText() : null);
        entity.setDiffUrl(pr.has("diff_url") ? pr.get("diff_url").asText() : null);
        entity.setPatchUrl(pr.has("patch_url") ? pr.get("patch_url").asText() : null);
        entity.setAdditions(pr.has("additions") ? pr.get("additions").asInt() : 0);
        entity.setDeletions(pr.has("deletions") ? pr.get("deletions").asInt() : 0);
        entity.setChangedFiles(pr.has("changed_files") ? pr.get("changed_files").asInt() : 0);
        if (pr.has("created_at") && !pr.get("created_at").isNull()) {
            try { entity.setGithubCreatedAt(LocalDateTime.parse(
                    pr.get("created_at").asText().replace("Z", ""))); } catch (Exception e) { /* ignore */ }
        }
        if (pr.has("updated_at") && !pr.get("updated_at").isNull()) {
            try { entity.setGithubUpdatedAt(LocalDateTime.parse(
                    pr.get("updated_at").asText().replace("Z", ""))); } catch (Exception e) { /* ignore */ }
        }
        entity.setUpdateTime(LocalDateTime.now());

        if (existing != null) {
            prCacheMapper.updateById(entity);
        } else {
            entity.setCreateTime(LocalDateTime.now());
            prCacheMapper.insert(entity);
        }
        return entity;
    }

    private GithubPullRequestResponse toResponse(GithubPullRequestCacheEntity entity) {
        GithubPullRequestResponse r = new GithubPullRequestResponse();
        r.setId(entity.getId() != null ? entity.getId().toString() : null);
        r.setGithubPrId(entity.getGithubPrId());
        r.setGithubRepoId(entity.getGithubRepoId());
        r.setNumber(entity.getNumber());
        r.setTitle(entity.getTitle());
        r.setState(entity.getState());
        r.setAuthorLogin(entity.getAuthorLogin());
        r.setBaseBranch(entity.getBaseBranch());
        r.setHeadBranch(entity.getHeadBranch());
        r.setHtmlUrl(entity.getHtmlUrl());
        r.setAdditions(entity.getAdditions());
        r.setDeletions(entity.getDeletions());
        r.setChangedFiles(entity.getChangedFiles());
        r.setGithubCreatedAt(entity.getGithubCreatedAt() != null ? entity.getGithubCreatedAt().toString() : null);
        r.setGithubUpdatedAt(entity.getGithubUpdatedAt() != null ? entity.getGithubUpdatedAt().toString() : null);
        return r;
    }
}
