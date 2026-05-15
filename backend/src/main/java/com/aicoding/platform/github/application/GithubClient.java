package com.aicoding.platform.github.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class GithubClient {

    private static final Logger log = LoggerFactory.getLogger(GithubClient.class);
    private static final String GITHUB_API_BASE = "https://api.github.com";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RestClient buildClient(String accessToken) {
        return RestClient.builder()
                .baseUrl(GITHUB_API_BASE)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader("User-Agent", "ai-coding-platform")
                .build();
    }

    private void handleError(int status, String body) {
        String key = body != null && body.length() > 200 ? body.substring(0, 200) : body;
        switch (status) {
            case 401, 403 -> throw new BizException(ErrorCode.GITHUB_AUTH_FAILED, "GitHub 认证失败: " + key);
            case 404 -> throw new BizException(ErrorCode.GITHUB_REPO_NOT_FOUND, "GitHub 资源不存在: " + key);
            case 429 -> throw new BizException(ErrorCode.GITHUB_RATE_LIMITED, "GitHub API 频率限制");
            default -> throw new BizException(ErrorCode.GITHUB_API_ERROR, "GitHub API HTTP " + status + ": " + key);
        }
    }

    public JsonNode getCurrentUser(String accessToken) {
        try {
            String json = buildClient(accessToken).get()
                    .uri("/user")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        byte[] bytes = res.getBody() != null ? res.getBody().readAllBytes() : new byte[0];
                        handleError(res.getStatusCode().value(), new String(bytes));
                    })
                    .body(String.class);
            return objectMapper.readTree(json);
        } catch (BizException e) { throw e;
        } catch (JsonProcessingException | RestClientException e) {
            log.error("GitHub /user failed: {}", e.getMessage());
            throw new BizException(ErrorCode.GITHUB_API_ERROR, "获取 GitHub 用户信息失败");
        }
    }

    public List<JsonNode> listRepositories(String accessToken) {
        List<JsonNode> allRepos = new ArrayList<>();
        int page = 1;
        while (page <= 10) {
            try {
                final int currentPage = page;
                String json = buildClient(accessToken).get()
                        .uri(uriBuilder -> uriBuilder.path("/user/repos")
                                .queryParam("per_page", "100")
                                .queryParam("page", String.valueOf(currentPage))
                                .queryParam("sort", "updated")
                                .build())
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (req, res) -> {
                            byte[] bytes = res.getBody() != null ? res.getBody().readAllBytes() : new byte[0];
                            handleError(res.getStatusCode().value(), new String(bytes));
                        })
                        .body(String.class);
                JsonNode arr = objectMapper.readTree(json);
                if (!arr.isArray() || arr.size() == 0) break;
                for (JsonNode node : arr) allRepos.add(node);
                if (arr.size() < 100) break;
                page++;
            } catch (BizException e) { throw e;
            } catch (JsonProcessingException | RestClientException e) {
                log.error("GitHub /user/repos page {} failed: {}", page, e.getMessage());
                throw new BizException(ErrorCode.GITHUB_API_ERROR, "获取仓库列表失败");
            }
        }
        return allRepos;
    }

    public List<JsonNode> listPullRequests(String accessToken, String owner, String repo, String state) {
        try {
            String json = buildClient(accessToken).get()
                    .uri(uriBuilder -> uriBuilder.path("/repos/{owner}/{repo}/pulls")
                            .queryParam("state", state != null ? state : "open")
                            .queryParam("per_page", "50")
                            .build(owner, repo))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        byte[] bytes = res.getBody() != null ? res.getBody().readAllBytes() : new byte[0];
                        handleError(res.getStatusCode().value(), new String(bytes));
                    })
                    .body(String.class);
            JsonNode arr = objectMapper.readTree(json);
            List<JsonNode> result = new ArrayList<>();
            if (arr.isArray()) for (JsonNode n : arr) result.add(n);
            return result;
        } catch (BizException e) { throw e;
        } catch (JsonProcessingException | RestClientException e) {
            log.error("GitHub /repos/{}/{}/pulls failed: {}", owner, repo, e.getMessage());
            throw new BizException(ErrorCode.GITHUB_API_ERROR, "获取 PR 列表失败");
        }
    }

    public JsonNode getPullRequest(String accessToken, String owner, String repo, int number) {
        try {
            String json = buildClient(accessToken).get()
                    .uri("/repos/{owner}/{repo}/pulls/{number}", owner, repo, number)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        byte[] bytes = res.getBody() != null ? res.getBody().readAllBytes() : new byte[0];
                        int status = res.getStatusCode().value();
                        if (status == 404) throw new BizException(ErrorCode.GITHUB_PR_NOT_FOUND, "PR #" + number + " 不存在");
                        handleError(status, new String(bytes));
                    })
                    .body(String.class);
            return objectMapper.readTree(json);
        } catch (BizException e) { throw e;
        } catch (JsonProcessingException | RestClientException e) {
            log.error("GitHub PR detail {}/{} #{} failed: {}", owner, repo, number, e.getMessage());
            throw new BizException(ErrorCode.GITHUB_API_ERROR, "获取 PR 详情失败");
        }
    }

    public List<JsonNode> listPullRequestFiles(String accessToken, String owner, String repo, int number) {
        try {
            String json = buildClient(accessToken).get()
                    .uri("/repos/{owner}/{repo}/pulls/{number}/files", owner, repo, number)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        byte[] bytes = res.getBody() != null ? res.getBody().readAllBytes() : new byte[0];
                        handleError(res.getStatusCode().value(), new String(bytes));
                    })
                    .body(String.class);
            JsonNode arr = objectMapper.readTree(json);
            List<JsonNode> result = new ArrayList<>();
            if (arr.isArray()) for (JsonNode n : arr) result.add(n);
            return result;
        } catch (BizException e) { throw e;
        } catch (JsonProcessingException | RestClientException e) {
            log.error("GitHub PR files {}/{} #{} failed: {}", owner, repo, number, e.getMessage());
            throw new BizException(ErrorCode.GITHUB_API_ERROR, "获取 PR 文件列表失败");
        }
    }

    public String getPullRequestPatch(String accessToken, String patchUrl) {
        try {
            return RestClient.create().get()
                    .uri(Objects.requireNonNull(patchUrl))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3.diff")
                    .header("User-Agent", "ai-coding-platform")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        byte[] bytes = res.getBody() != null ? res.getBody().readAllBytes() : new byte[0];
                        handleError(res.getStatusCode().value(), new String(bytes));
                    })
                    .body(String.class);
        } catch (BizException e) { throw e;
        } catch (RestClientException e) {
            log.error("GitHub patch download failed: {}", e.getMessage());
            throw new BizException(ErrorCode.GITHUB_API_ERROR, "获取 PR patch 失败");
        }
    }
}
