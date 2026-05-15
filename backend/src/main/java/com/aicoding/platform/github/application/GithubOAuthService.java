package com.aicoding.platform.github.application;

import com.aicoding.platform.audit.application.AuditLogApplicationService;
import com.aicoding.platform.audit.domain.AuditActionType;
import com.aicoding.platform.auth.domain.GithubAccountEntity;
import com.aicoding.platform.auth.domain.GithubAccountStatus;
import com.aicoding.platform.auth.infrastructure.GithubAccountMapper;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.github.domain.GithubOAuthStateEntity;
import com.aicoding.platform.github.domain.GithubOAuthStateStatus;
import com.aicoding.platform.github.dto.GithubOAuthAuthorizeResponse;
import com.aicoding.platform.github.dto.GithubOAuthStatusResponse;
import com.aicoding.platform.github.infrastructure.GithubOAuthStateMapper;
import com.aicoding.platform.security.context.LoginUserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
public class GithubOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GithubOAuthService.class);
    private static final @NonNull MediaType FORM_URLENCODED = Objects.requireNonNull(MediaType.APPLICATION_FORM_URLENCODED);

    private final GithubProperties properties;
    private final GithubOAuthStateMapper stateMapper;
    private final GithubAccountMapper githubAccountMapper;
    private final AuditLogApplicationService auditLogApplicationService;
    private final GithubClient githubClient;

    public GithubOAuthService(GithubProperties properties,
                              GithubOAuthStateMapper stateMapper,
                              GithubAccountMapper githubAccountMapper,
                              AuditLogApplicationService auditLogApplicationService,
                              GithubClient githubClient) {
        this.properties = properties;
        this.stateMapper = stateMapper;
        this.githubAccountMapper = githubAccountMapper;
        this.auditLogApplicationService = auditLogApplicationService;
        this.githubClient = githubClient;
    }

    public GithubOAuthAuthorizeResponse authorize() {
        Long userId = LoginUserContext.currentUserId();
        if (userId == null) throw new BizException(ErrorCode.UNAUTHORIZED);

        if (!properties.isConfigured()) {
            GithubOAuthAuthorizeResponse resp = new GithubOAuthAuthorizeResponse();
            resp.setConfigured(false);
            return resp;
        }

        String state = UUID.randomUUID().toString().replace("-", "");

        GithubOAuthStateEntity entity = new GithubOAuthStateEntity();
        entity.setState(state);
        entity.setUserId(userId);
        entity.setStatus(GithubOAuthStateStatus.PENDING.name());
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        entity.setCreateTime(LocalDateTime.now());
        stateMapper.insert(entity);

        String url = "https://github.com/login/oauth/authorize"
                + "?client_id=" + properties.getClientId()
                + "&redirect_uri=" + properties.getRedirectUri()
                + "&scope=" + properties.getScopes()
                + "&state=" + state;

        auditLogApplicationService.recordSuccess(null, null,
                AuditActionType.GITHUB_OAUTH_START.name(), "GITHUB_OAUTH",
                "用户发起 GitHub OAuth 授权");

        GithubOAuthAuthorizeResponse resp = new GithubOAuthAuthorizeResponse();
        resp.setConfigured(true);
        resp.setAuthorizeUrl(url);
        resp.setState(state);
        return resp;
    }

    @Transactional
    public String callback(String code, String state) {
        if (!properties.isConfigured()) {
            throw new BizException(ErrorCode.GITHUB_OAUTH_NOT_CONFIGURED);
        }

        // Validate state
        GithubOAuthStateEntity stateEntity = stateMapper.selectOne(
                new LambdaQueryWrapper<GithubOAuthStateEntity>()
                        .eq(GithubOAuthStateEntity::getState, state));

        if (stateEntity == null) {
            throw new BizException(ErrorCode.GITHUB_STATE_INVALID, "OAuth state 无效");
        }
        if (stateEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            stateEntity.setStatus(GithubOAuthStateStatus.EXPIRED.name());
            stateMapper.updateById(stateEntity);
            throw new BizException(ErrorCode.GITHUB_STATE_INVALID, "OAuth state 已过期");
        }
        if (!GithubOAuthStateStatus.PENDING.name().equals(stateEntity.getStatus())) {
            throw new BizException(ErrorCode.GITHUB_STATE_INVALID, "OAuth state 已使用");
        }

        // Exchange code for token
        String accessToken;
        try {
            accessToken = exchangeCodeForToken(code);
        } catch (BizException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("GitHub token exchange failed: {}", e.getMessage());
            throw new BizException(ErrorCode.GITHUB_TOKEN_EXCHANGE_FAILED, "Token 交换失败");
        }

        // Get GitHub user info
        JsonNode githubUser = githubClient.getCurrentUser(accessToken);
        String githubLogin = githubUser.has("login") ? githubUser.get("login").asText() : "";
        String githubId = githubUser.has("id") ? githubUser.get("id").asText() : "";

        // Upsert github_account binding
        Long userId = stateEntity.getUserId();
        GithubAccountEntity account = githubAccountMapper.selectOne(
                new LambdaQueryWrapper<GithubAccountEntity>()
                        .eq(GithubAccountEntity::getUserId, userId));

        boolean isNew = account == null;
        GithubAccountEntity boundAccount = account;
        if (boundAccount == null) {
            boundAccount = new GithubAccountEntity();
            boundAccount.setUserId(userId);
            boundAccount.setBindTime(LocalDateTime.now());
            boundAccount.setCreateTime(LocalDateTime.now());
        }
        boundAccount.setGithubId(githubId);
        boundAccount.setLogin(githubLogin);
        boundAccount.setAccessTokenEnc(accessToken);
        boundAccount.setScope(properties.getScopes());
        boundAccount.setStatus(GithubAccountStatus.BOUND.name());
        boundAccount.setUpdateTime(LocalDateTime.now());

        if (isNew) {
            githubAccountMapper.insert(boundAccount);
        } else {
            githubAccountMapper.updateById(boundAccount);
        }

        // Mark state as USED
        stateEntity.setStatus(GithubOAuthStateStatus.USED.name());
        stateMapper.updateById(stateEntity);

        auditLogApplicationService.recordSuccess(null, null,
                AuditActionType.GITHUB_OAUTH_CALLBACK.name(), "GITHUB_OAUTH",
                "GitHub 账号绑定成功: " + githubLogin);

        log.info("GitHub OAuth callback success: login={} userId={}", githubLogin, userId);
        return "<html><body style=\"text-align:center;padding-top:80px;font-family:sans-serif;\">"
                + "<h1>✅ 授权成功</h1>"
                + "<p>GitHub 账号 <strong>" + githubLogin + "</strong> 已绑定到平台。</p>"
                + "<p>此窗口可以关闭。</p>"
                + "</body></html>";
    }

    private String exchangeCodeForToken(String code) {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://github.com")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "ai-coding-platform")
                .build();

        String body = "client_id=" + properties.getClientId()
                + "&client_secret=" + properties.getClientSecret()
                + "&code=" + code
                + "&redirect_uri=" + properties.getRedirectUri();

        String json = restClient.post()
                .uri("/login/oauth/access_token")
                .contentType(FORM_URLENCODED)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    byte[] bytes = res.getBody() != null ? res.getBody().readAllBytes() : new byte[0];
                    throw new BizException(ErrorCode.GITHUB_TOKEN_EXCHANGE_FAILED,
                            "Token 交换 HTTP " + res.getStatusCode().value() + ": " + new String(bytes));
                })
                .body(String.class);

        try {
            JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            if (root.has("error")) {
                String err = root.get("error").asText();
                String errDesc = root.has("error_description") ? root.get("error_description").asText() : "";
                throw new BizException(ErrorCode.GITHUB_TOKEN_EXCHANGE_FAILED, err + ": " + errDesc);
            }
            if (!root.has("access_token")) {
                throw new BizException(ErrorCode.GITHUB_TOKEN_EXCHANGE_FAILED, "响应缺少 access_token");
            }
            return root.get("access_token").asText();
        } catch (BizException e) { throw e;
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.GITHUB_TOKEN_EXCHANGE_FAILED, "Token 响应解析失败");
        }
    }

    public GithubOAuthStatusResponse status() {
        Long userId = LoginUserContext.currentUserId();
        if (userId == null) throw new BizException(ErrorCode.UNAUTHORIZED);

        GithubOAuthStatusResponse resp = new GithubOAuthStatusResponse();
        resp.setConfigured(properties.isConfigured());

        GithubAccountEntity account = githubAccountMapper.selectOne(
                new LambdaQueryWrapper<GithubAccountEntity>()
                        .eq(GithubAccountEntity::getUserId, userId)
                        .eq(GithubAccountEntity::getStatus, GithubAccountStatus.BOUND.name()));

        if (account != null) {
            resp.setBound(true);
            resp.setGithubLogin(account.getLogin());
            resp.setGithubUserId(Long.valueOf(account.getGithubId()));
        }
        return resp;
    }

    public void unbind(Long bindingId) {
        Long userId = LoginUserContext.currentUserId();
        if (userId == null) throw new BizException(ErrorCode.UNAUTHORIZED);

        GithubAccountEntity account = githubAccountMapper.selectById(bindingId);
        if (account == null || !account.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_FOUND, "绑定记录不存在");
        }
        account.setStatus(GithubAccountStatus.REVOKED.name());
        account.setAccessTokenEnc("");
        account.setUpdateTime(LocalDateTime.now());
        githubAccountMapper.updateById(account);

        auditLogApplicationService.recordSuccess(null, null,
                AuditActionType.GITHUB_OAUTH_CALLBACK.name(), "GITHUB_OAUTH",
                "GitHub 账号解绑: " + account.getLogin());
    }

    String getAccessToken(Long userId) {
        GithubAccountEntity account = githubAccountMapper.selectOne(
                new LambdaQueryWrapper<GithubAccountEntity>()
                        .eq(GithubAccountEntity::getUserId, userId)
                        .eq(GithubAccountEntity::getStatus, GithubAccountStatus.BOUND.name()));

        if (account == null || account.getAccessTokenEnc() == null || account.getAccessTokenEnc().isBlank()) {
            throw new BizException(ErrorCode.GITHUB_TOKEN_MISSING);
        }
        return account.getAccessTokenEnc();
    }
}
