# Milestone 32: Redis 基础设施接入与认证安全增强

## 1. 背景

当前项目已经完成：

- JWT 登录认证。
- 登录验证码（Milestone 31）。
- 审计日志与可观测性。
- Docker Compose 中已有 Redis 服务占位。

但后端当前仍未真正引入 Redis 依赖，验证码、登录状态、限流、短期缓存等能力还不能跨实例共享。

本阶段目标是把 Redis 作为基础设施正式接入，并优先落地到认证安全场景。

## 2. Redis 可以接入的位置

Redis 在本项目里可以用于以下场景：

| 场景 | Redis Key 示例 | 优先级 | 说明 |
|---|---|---:|---|
| 登录验证码 | `auth:captcha:{captchaId}` | P0 | 替换内存验证码，支持多实例 |
| 登录失败计数 | `auth:login:fail:email:{email}` / `auth:login:fail:ip:{ip}` | P0 | 防暴力破解 |
| 登录临时锁定 | `auth:login:lock:email:{email}` / `auth:login:lock:ip:{ip}` | P0 | 失败过多短期锁定 |
| JWT 黑名单 | `auth:jwt:blacklist:{jti}` | P1 | logout 后 token 立即失效 |
| 用户在线状态 | `auth:online:user:{userId}` | P2 | 在线用户、设备管理 |
| Chat SSE 状态 | `chat:stream:{messageId}` | P2 | 流式执行状态缓存 |
| Task / Agent 执行锁 | `task:running:{taskId}` | P2 | 防重复执行 |
| RAG 搜索缓存 | `rag:search:{projectId}:{queryHash}` | P3 | 减少重复检索 |
| Model Provider 健康缓存 | `model:provider:health:{provider}` | P3 | 快速 fallback |
| GitHub OAuth state | `github:oauth:state:{state}` | P2 | OAuth state TTL 管理 |

本阶段建议只做 P0：

1. Redis 基础设施接入。
2. 验证码从内存迁移到 Redis。
3. 登录失败次数与临时锁定。

JWT 黑名单作为后续阶段，不在本阶段强制实现。

## 3. 严格边界

执行本阶段必须遵守：

1. 不改 JWT token 结构，暂不新增 `jti`。
2. 不改 refresh token 逻辑。
3. 不改 Chat / RAG / Model Gateway 业务逻辑。
4. 不引入 Redisson，先使用 Spring Data Redis。
5. 不把 Redis 作为测试必须依赖，测试环境可使用 fake / mock / disabled 配置。
6. 不把验证码答案写入日志。
7. 不在前端暴露锁定策略细节。
8. 不删除 Milestone 31 已有验证码测试。
9. 不破坏现有 162 个后端测试和 14 个前端 E2E。

允许做：

- 新增 Redis Maven 依赖。
- 新增 Redis 配置。
- 新增 Redis key 命名常量。
- 修改 CaptchaService 存储实现。
- 新增 LoginAttemptService。
- 修改 AuthApplicationService.login() 增加失败计数和临时锁定。
- 新增测试 profile 配置。
- 修改 Docker / README / env 示例。

## 4. 总体目标

实现 4 个能力：

1. Redis Foundation
   - 后端引入 Redis starter。
   - application.yml / .env.example / docker compose 配置完整。
   - Redis health check 正常。

2. Redis Captcha Store
   - 验证码存储从 ConcurrentHashMap 迁移到 Redis。
   - 使用 Redis TTL 管理过期。
   - 校验成功后删除。
   - 超过最大尝试次数后删除。

3. Login Attempt Protection
   - 按 email 统计失败次数。
   - 按 IP 统计失败次数。
   - 达到阈值后临时锁定。
   - 登录成功后清理失败计数。

4. Test & Docs
   - 后端测试覆盖 Redis 相关逻辑。
   - 前端无需新增 UI，只保持现有错误提示可用。
   - 文档说明 Redis 用途、配置、key 命名、限制。

## 5. Redis 配置设计

### 5.1 Maven 依赖

修改：

```text
backend/pom.xml
```

新增：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 5.2 application.yml

修改：

```text
backend/src/main/resources/application.yml
```

新增或启用：

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DB:0}
      timeout: 3000ms
```

注意当前文件底部已有 Redis 占位注释，需要转为真实配置，避免出现重复 `spring:` 根节点。

### 5.3 自定义配置

在 `app.auth` 下增加登录安全配置：

```yaml
app:
  auth:
    captcha:
      enabled: ${AUTH_CAPTCHA_ENABLED:true}
      expire-seconds: ${AUTH_CAPTCHA_EXPIRE_SECONDS:120}
      length: ${AUTH_CAPTCHA_LENGTH:4}
      max-attempts: ${AUTH_CAPTCHA_MAX_ATTEMPTS:3}
      width: ${AUTH_CAPTCHA_WIDTH:120}
      height: ${AUTH_CAPTCHA_HEIGHT:40}
      store: ${AUTH_CAPTCHA_STORE:redis}
    login-protection:
      enabled: ${AUTH_LOGIN_PROTECTION_ENABLED:true}
      max-email-failures: ${AUTH_LOGIN_MAX_EMAIL_FAILURES:5}
      max-ip-failures: ${AUTH_LOGIN_MAX_IP_FAILURES:20}
      failure-window-seconds: ${AUTH_LOGIN_FAILURE_WINDOW_SECONDS:300}
      lock-seconds: ${AUTH_LOGIN_LOCK_SECONDS:600}
```

说明：

- `captcha.store=redis`：默认使用 Redis。
- 可选支持 `memory` 作为 fallback，方便测试或无 Redis 本地运行。
- 登录失败窗口默认 5 分钟。
- email 失败 5 次锁 10 分钟。
- IP 失败 20 次锁 10 分钟。

### 5.4 .env.example

修改：

```text
.env.example
```

新增：

```env
# ---- Redis ----
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DB=0

# ---- Auth Protection ----
AUTH_CAPTCHA_ENABLED=true
AUTH_CAPTCHA_STORE=redis
AUTH_CAPTCHA_EXPIRE_SECONDS=120
AUTH_CAPTCHA_MAX_ATTEMPTS=3
AUTH_LOGIN_PROTECTION_ENABLED=true
AUTH_LOGIN_MAX_EMAIL_FAILURES=5
AUTH_LOGIN_MAX_IP_FAILURES=20
AUTH_LOGIN_FAILURE_WINDOW_SECONDS=300
AUTH_LOGIN_LOCK_SECONDS=600
```

## 6. Redis Key 规范

建议新增：

```text
backend/src/main/java/com/aicoding/platform/common/redis/RedisKeys.java
```

内容：

```java
public final class RedisKeys {
    private static final String PREFIX = "ai-coding-platform:";

    public static String captcha(String captchaId) {
        return PREFIX + "auth:captcha:" + captchaId;
    }

    public static String loginFailEmail(String email) {
        return PREFIX + "auth:login:fail:email:" + email.toLowerCase();
    }

    public static String loginFailIp(String ip) {
        return PREFIX + "auth:login:fail:ip:" + ip;
    }

    public static String loginLockEmail(String email) {
        return PREFIX + "auth:login:lock:email:" + email.toLowerCase();
    }

    public static String loginLockIp(String ip) {
        return PREFIX + "auth:login:lock:ip:" + ip;
    }
}
```

注意：

- 不要把 raw password/token/API key 放进 key。
- email 需要 trim + lower-case。
- IP 从 request 中解析，优先 `X-Forwarded-For`、`X-Real-IP`，否则 remote addr。

## 7. 验证码 Redis 改造

### 7.1 建议 DTO / 内部模型

验证码 Redis value 可以用 JSON：

```json
{
  "code": "A7K2",
  "attempts": 0
}
```

也可以用 Hash：

```text
HSET auth:captcha:{id} code A7K2 attempts 0
EXPIRE auth:captcha:{id} 120
```

推荐 JSON + StringRedisTemplate，简单且足够：

- 写入：`opsForValue().set(key, json, ttl)`
- 读取：`opsForValue().get(key)`
- 删除：`delete(key)`
- 更新失败次数：重新 set 剩余 TTL 或使用 Hash。

如果要避免重新计算 TTL，推荐 Hash + expire。

### 7.2 CaptchaService 修改

文件：

```text
backend/src/main/java/com/aicoding/platform/auth/application/CaptchaService.java
```

目标：

- 注入 `StringRedisTemplate`。
- 根据 `CaptchaProperties.store` 选择 Redis / memory。
- Redis 可用时使用 Redis 存储。
- Redis 不可用时是否 fallback 到 memory 由实现决定，但必须记录 warn，不影响测试。

建议实现：

1. `generate()`
   - 生成 captchaId。
   - 生成 code。
   - 写 Redis key，TTL = expireSeconds。
   - 返回图片。

2. `validate(captchaId, captchaCode)`
   - 如果验证码关闭，直接 return。
   - captchaId / captchaCode 缺失：`CAPTCHA_REQUIRED`。
   - Redis key 不存在：`CAPTCHA_EXPIRED`。
   - code 不匹配：attempts + 1；超过上限删除；抛 `CAPTCHA_INVALID`。
   - code 匹配：删除 key；return。

## 8. 登录失败保护

### 8.1 新增配置类

```text
backend/src/main/java/com/aicoding/platform/auth/config/LoginProtectionProperties.java
```

配置：

- enabled
- maxEmailFailures
- maxIpFailures
- failureWindowSeconds
- lockSeconds

### 8.2 新增 Service

```text
backend/src/main/java/com/aicoding/platform/auth/application/LoginAttemptService.java
```

方法：

```java
void checkLocked(String email, String ip);
void recordFailure(String email, String ip);
void recordSuccess(String email, String ip);
```

行为：

- `checkLocked()`：如果 email 或 IP 锁定，抛 `AUTH_TOO_MANY_ATTEMPTS`。
- `recordFailure()`：
  - email fail count +1，首次设置 TTL。
  - ip fail count +1，首次设置 TTL。
  - 达阈值则设置 lock key，TTL = lockSeconds。
- `recordSuccess()`：
  - 删除 email/ip fail key。
  - 不一定删除 lock key；如果登录已经成功，说明未被锁，可删除 fail key 即可。

### 8.3 ErrorCode

建议新增：

```java
AUTH_TOO_MANY_ATTEMPTS
```

HTTP 状态：

```text
429 Too Many Requests
```

如果当前 ErrorCode 不支持 429，就使用 400 / 401，但推荐补 429。

### 8.4 AuthApplicationService 修改

文件：

```text
backend/src/main/java/com/aicoding/platform/auth/application/AuthApplicationService.java
```

问题：当前 `login(LoginRequest request)` 可能拿不到 IP。

推荐方案：

- 在 Controller 层获取 IP，传入 service。
- 或在 LoginAttemptService 内通过 RequestContextHolder 获取当前请求。

更推荐最小改动：

```java
loginAttemptService.checkLocked(request.getEmail(), loginAttemptService.currentClientIp());
captchaService.validate(...);
try {
    // 原登录逻辑
    loginAttemptService.recordSuccess(request.getEmail(), ip);
    return response;
} catch (BizException ex) {
    if (isAuthFailure(ex)) {
        loginAttemptService.recordFailure(request.getEmail(), ip);
    }
    throw ex;
}
```

注意：

- 验证码失败是否计入登录失败：建议计入 IP，不计入 email，避免攻击者用不存在 email 污染计数。
- 账号密码错误计入 email + IP。
- 登录成功清理失败计数。

如果现有代码里密码错误抛 `UNAUTHORIZED`，只对该错误记录失败。

## 9. 测试设计

### 9.1 后端测试

新增或修改：

```text
backend/src/test/java/com/aicoding/platform/auth/CaptchaServiceTest.java
backend/src/test/java/com/aicoding/platform/auth/CaptchaIntegrationTest.java
backend/src/test/java/com/aicoding/platform/auth/LoginAttemptServiceTest.java
backend/src/test/java/com/aicoding/platform/auth/AuthIntegrationTest.java
```

必须覆盖：

1. Captcha Redis key 写入并带 TTL。
2. 正确验证码校验后 key 删除。
3. 错误验证码 attempts 增加。
4. 超过最大尝试次数后 key 删除。
5. 过期 / 不存在验证码返回 CAPTCHA_EXPIRED。
6. 登录失败次数达到阈值后返回 AUTH_TOO_MANY_ATTEMPTS。
7. 登录成功后清理失败计数。
8. 测试环境 `AUTH_CAPTCHA_ENABLED=false` 时旧登录 helper 仍可用。

### 9.2 测试环境策略

为了不让 `mvn test` 依赖本机 Redis，推荐：

- 单元测试使用 mock `StringRedisTemplate` 或封装 `RedisStringStore`。
- 集成测试 profile 设置：

```yaml
app:
  auth:
    captcha:
      enabled: false
      store: memory
    login-protection:
      enabled: false
```

或者引入 embedded Redis / Testcontainers，但本阶段不强制。

当前项目已有 MySQL 测试依赖，不建议再强加 Redis 外部依赖。

### 9.3 前端测试

前端 UI 不需要变化；验证码 UI 已在 Milestone 31 完成。

如果登录保护返回 429，前端应显示后端 message。

E2E 必须继续通过：

```bash
cd frontend
npm run test:e2e -- --workers=1
```

## 10. 文档更新

建议修改：

```text
README.md
.env.example
docs/environment-variable-index.md
docs/production-deployment-runbook.md
docs/final-delivery-report.md
docs/testing-strategy.md
```

最少要更新：

- `.env.example`
- `README.md`
- `docs/environment-variable-index.md`

说明：

- Redis 配置项。
- 验证码默认使用 Redis。
- 本地 Docker Compose 已包含 Redis。
- 如果不想启用 Redis，可设置 `AUTH_CAPTCHA_STORE=memory`。

## 11. 必须执行的验证命令

后端：

```bash
cd backend
mvn test
```

前端：

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

如果本地 Redis 可用，额外验证：

```bash
redis-cli ping
redis-cli keys 'ai-coding-platform:*'
```

手动验证：

1. 启动 Redis。
2. 启动后端。
3. 打开登录页。
4. 获取验证码。
5. 登录失败几次后触发锁定。
6. 等待锁定过期后可重新登录。
7. 正确登录后失败计数清理。

## 12. 验收标准

功能验收：

- Redis health 正常。
- 验证码使用 Redis TTL。
- 验证码成功后一次性删除。
- 错误验证码 attempts 生效。
- 登录失败达到阈值后被临时锁定。
- 登录成功后清理失败计数。
- Redis 不可用时错误清晰，或按配置 fallback memory。

安全验收：

- 不记录验证码答案。
- 不记录密码。
- 不把 IP 锁定细节暴露给攻击者。
- 不把 Redis key 泄露到前端。
- `.env` 不被提交。

回归验收：

- `mvn test` 通过。
- `npm run typecheck` 通过。
- `npm run build` 通过。
- E2E 通过。
- Chat SSE、Model Gateway、GitHub 页面不受影响。

## 13. 完成报告格式

完成后按以下格式输出：

1. 新增 / 修改文件清单
2. Redis 基础设施接入说明
3. Captcha Redis 存储改造说明
4. 登录失败限流 / 锁定说明
5. Redis key 规范
6. 配置项说明
7. 后端测试结果
8. 前端 typecheck / build / E2E 结果
9. 手动 Redis 验证结果
10. 已知限制
11. 是否可以进入下一阶段

---

# Claude 执行提示词

请根据项目中的文档执行 Milestone 32。

文档路径：

```text
docs/milestone-32-redis-foundation-auth-hardening.md
```

执行要求：

1. 先完整阅读该文档，再检查当前 backend / frontend 代码结构。
2. 本阶段目标是 Redis 基础设施接入、验证码 Redis 存储、登录失败限流，不要扩散到 Chat/RAG/Model Gateway。
3. 不要重写 Auth / JWT / Security 架构。
4. 不要改 refresh token 逻辑。
5. 不要新增 Redisson，优先使用 Spring Data Redis。
6. 不要让 `mvn test` 强依赖本机 Redis。
7. 不要把验证码答案、密码、token 写入日志。
8. 不要把 Redis key 或锁定细节暴露给前端。
9. 不要删除 Milestone 31 的验证码测试。
10. 如果测试因登录保护失败，优先调整 test profile 或测试 helper，不要删除断言。

需要实现：

1. `spring-boot-starter-data-redis` 依赖。
2. `spring.data.redis` 配置。
3. `.env.example` Redis 与登录保护配置。
4. `RedisKeys` key 命名工具。
5. `CaptchaService` 从内存存储迁移到 Redis，并保留 memory fallback 或 test profile。
6. `LoginProtectionProperties`。
7. `LoginAttemptService`，支持 email/IP 失败计数和临时锁定。
8. `AuthApplicationService.login()` 接入锁定检查、失败记录、成功清理。
9. `ErrorCode` 增加 `AUTH_TOO_MANY_ATTEMPTS`。
10. 后端测试覆盖 Redis captcha 与登录限流。
11. 文档更新：`.env.example`、`README.md`、`docs/environment-variable-index.md`。

完成后必须执行：

```bash
cd backend
mvn test

cd ../frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

完成后按文档第 13 节格式输出报告。

现在开始实现，不要只给计划。
