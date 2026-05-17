# Milestone 31: 登录验证码与认证安全增强

## 1. 背景

当前项目已经完成 v1.0 Alpha 交付，登录流程具备：

- 邮箱 + 密码登录。
- JWT access / refresh token。
- 登录态保护前端路由。
- 后端统一 401 / 403 响应。

新增需求：在登录界面加入验证码功能，降低弱口令暴力尝试风险，并提升演示系统的安全完整度。

本阶段只做登录验证码，不引入短信验证码、邮箱验证码、注册流程、第三方图形验证码服务。

## 2. 目标

实现一个轻量、可配置、可测试的登录验证码闭环：

1. 后端生成验证码
   - 提供验证码获取接口。
   - 返回 `captchaId` 和 Base64 图片。
   - 服务端保存验证码答案、过期时间、尝试次数。

2. 登录校验验证码
   - `/api/auth/login` 必须携带 `captchaId` 和 `captchaCode`。
   - 验证码错误、过期、缺失时拒绝登录。
   - 验证码校验成功后一次性失效。

3. 前端登录页展示验证码
   - 登录页新增验证码输入框。
   - 显示验证码图片。
   - 支持点击刷新验证码。
   - 登录失败后自动刷新验证码。

4. 配置与测试
   - 支持配置开关、过期时间、长度、最大尝试次数。
   - 后端集成测试覆盖验证码正向和负向路径。
   - 前端 E2E 登录测试适配验证码。

## 3. 严格边界

执行本阶段必须遵守：

1. 不改 Auth 已有核心 JWT 逻辑。
2. 不改用户密码加密逻辑。
3. 不改 refresh token 流程。
4. 不引入真实短信、邮箱、第三方验证码服务。
5. 不引入 Redis 作为强依赖。
6. 不改变 ApiResponse 统一响应格式。
7. 不破坏现有 E2E 登录流程。
8. 不在接口中返回验证码明文答案。
9. 不把验证码答案写入日志。
10. 不提交真实密钥。

允许做：

- 新增验证码 Controller / Service / DTO。
- 修改 LoginRequest 增加验证码字段。
- 修改 AuthApplicationService.login() 增加验证码校验。
- 修改 SecurityConfig 放行验证码接口。
- 修改前端 LoginPage、auth api、auth store。
- 修改测试 helper，让登录测试自动先获取验证码。
- 新增配置项和文档说明。

## 4. 推荐技术方案

### 4.1 后端验证码类型

使用本地内存图形验证码：

- 字符集：去掉易混字符后的数字 + 大写字母，例如 `23456789ABCDEFGHJKLMNPQRSTUVWXYZ`。
- 长度：默认 4。
- 图片：Java2D `BufferedImage` 生成 PNG。
- 返回格式：Base64 Data URL。
- 存储：`ConcurrentHashMap<String, CaptchaEntry>`。
- TTL：默认 120 秒。
- 最大尝试次数：默认 3 次。
- 校验：大小写不敏感。
- 清理：获取/校验时懒清理过期数据即可，不要求定时任务。

> 当前项目是单机演示与本地部署优先，内存验证码足够。生产多实例部署时可替换为 Redis，接口和 DTO 不变。

### 4.2 API 设计

新增接口：

```http
GET /api/auth/captcha
```

响应：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "captchaId": "e7a0f8f8f6b64d2b8e5e6f2e7a9f1c20",
    "imageBase64": "data:image/png;base64,iVBORw0KGgo...",
    "expireSeconds": 120
  },
  "traceId": "...",
  "timestamp": "..."
}
```

修改登录接口：

```http
POST /api/auth/login
```

请求体由：

```json
{
  "email": "admin@example.com",
  "password": "Admin@123456"
}
```

改为：

```json
{
  "email": "admin@example.com",
  "password": "Admin@123456",
  "captchaId": "e7a0f8f8f6b64d2b8e5e6f2e7a9f1c20",
  "captchaCode": "A7K2"
}
```

验证码相关错误：

- 缺少验证码：`VALIDATION_ERROR` 或新增 `CAPTCHA_REQUIRED`。
- 验证码错误：建议新增 `CAPTCHA_INVALID`。
- 验证码过期：建议新增 `CAPTCHA_EXPIRED`。

如果项目现有 `ErrorCode` 适合扩展，优先新增：

```java
CAPTCHA_REQUIRED
CAPTCHA_INVALID
CAPTCHA_EXPIRED
```

HTTP 状态建议：

- 缺少验证码：400。
- 错误 / 过期：401 或 400。

建议统一为 400，避免和账号密码认证失败混淆。

## 5. 后端需要新增 / 修改文件

### 5.1 新增 DTO

```text
backend/src/main/java/com/aicoding/platform/auth/dto/CaptchaResponse.java
```

字段：

- `String captchaId`
- `String imageBase64`
- `Integer expireSeconds`

手写 getter/setter，不使用 Lombok。

### 5.2 新增配置类

```text
backend/src/main/java/com/aicoding/platform/auth/config/CaptchaProperties.java
```

配置前缀：

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
```

### 5.3 新增验证码服务

```text
backend/src/main/java/com/aicoding/platform/auth/application/CaptchaService.java
```

职责：

- `CaptchaResponse generate()`
- `void validate(String captchaId, String captchaCode)`
- 生成随机验证码。
- 生成 PNG Base64。
- 保存内存 entry。
- 校验过期、错误、尝试次数。
- 成功后删除。
- 超过最大尝试次数后删除。

安全要求：

- 不打印验证码答案。
- 不把答案返回给前端。
- 使用 `SecureRandom`。
- 校验时 `trim()` 并大小写不敏感。

### 5.4 新增 Controller

```text
backend/src/main/java/com/aicoding/platform/auth/controller/CaptchaController.java
```

接口：

```java
@GetMapping("/api/auth/captcha")
public ApiResponse<CaptchaResponse> captcha()
```

### 5.5 修改 LoginRequest

```text
backend/src/main/java/com/aicoding/platform/auth/dto/LoginRequest.java
```

新增：

```java
private String captchaId;
private String captchaCode;
```

如果验证码默认开启，可加 `@NotBlank`。如果要支持配置关闭，则不要用强注解，在 service 中按 `enabled` 决定是否校验。

推荐：

- 不加 `@NotBlank`。
- 在 `CaptchaService.validate()` 中处理缺失。

### 5.6 修改 AuthApplicationService

```text
backend/src/main/java/com/aicoding/platform/auth/application/AuthApplicationService.java
```

在 `login(LoginRequest request)` 中，账号密码校验前执行：

```java
captchaService.validate(request.getCaptchaId(), request.getCaptchaCode());
```

原因：

- 验证码先校验，减少无效密码尝试。
- 验证码错误时不暴露账号是否存在。

### 5.7 修改 SecurityConfig

```text
backend/src/main/java/com/aicoding/platform/security/config/SecurityConfig.java
```

放行：

```java
"/api/auth/captcha"
```

### 5.8 修改 application.yml

```text
backend/src/main/resources/application.yml
```

新增：

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
```

注意不要覆盖现有 `app.jwt`、`app.model-gateway`、`app.github`、`app.rag` 配置。

### 5.9 修改 ErrorCode

```text
backend/src/main/java/com/aicoding/platform/common/exception/ErrorCode.java
```

建议新增：

```java
CAPTCHA_REQUIRED
CAPTCHA_INVALID
CAPTCHA_EXPIRED
```

如现有枚举带 message / httpStatus，按项目现有风格补齐。

## 6. 前端需要新增 / 修改文件

### 6.1 修改 Auth API

```text
frontend/src/modules/auth/api.ts
```

新增：

```ts
export interface CaptchaResponse {
  captchaId: string
  imageBase64: string
  expireSeconds: number
}

export function getCaptcha() {
  return client.get<ApiResponse<CaptchaResponse>>('/api/auth/captcha')
}
```

修改登录请求类型：

```ts
captchaId?: string
captchaCode?: string
```

### 6.2 修改 Auth Store

```text
frontend/src/modules/auth/store.ts
```

让 `loginAction()` 支持验证码字段透传：

```ts
loginAction(payload: {
  email: string
  password: string
  captchaId?: string
  captchaCode?: string
})
```

不要在 store 中写死验证码逻辑，验证码 UI 状态留在 LoginPage。

### 6.3 修改 LoginPage

```text
frontend/src/modules/auth/pages/LoginPage.vue
```

新增 UI：

- 验证码输入框。
- 验证码图片。
- 刷新按钮。
- 加载状态。

建议布局：

```text
邮箱输入框
密码输入框
验证码输入框 + 图片 + 刷新按钮
错误提示
登录按钮
```

交互：

1. 页面 mounted 时调用 `loadCaptcha()`。
2. 点击验证码图片或刷新按钮重新获取。
3. 登录时提交 `captchaId` + `captchaCode`。
4. 登录失败时刷新验证码并清空 `captchaCode`。
5. 验证码加载失败时提示“验证码加载失败，请刷新”。

可访问性：

- 图片 `alt="验证码"`。
- 刷新按钮有明确 title。
- 输入框 placeholder 为“请输入验证码”。

## 7. 测试要求

### 7.1 后端测试

修改或新增：

```text
backend/src/test/java/com/aicoding/platform/auth/AuthIntegrationTest.java
backend/src/test/java/com/aicoding/platform/auth/CaptchaIntegrationTest.java
```

必须覆盖：

1. `GET /api/auth/captcha` 返回 captchaId、imageBase64、expireSeconds。
2. captcha imageBase64 以 `data:image/png;base64,` 开头。
3. 缺少 captchaId / captchaCode 登录失败。
4. 错误 captchaCode 登录失败。
5. 正确 captchaCode 登录成功。
6. 同一个 captcha 不能重复使用。
7. 过期验证码失败（可通过短 TTL test profile 或直接调用 service 测试）。

如果 integration test 无法获取验证码明文答案，允许：

- 给 `CaptchaService` 增加 package-private 测试辅助方法不可取。
- 更推荐在 `application-test.yml` 中设置 `AUTH_CAPTCHA_ENABLED=false` 给旧 Auth 测试，同时单独用 service 单元测试覆盖验证码逻辑。

但最终至少要有一个集成测试覆盖：

- captcha endpoint 可访问。
- 开启验证码时登录缺失验证码失败。

### 7.2 前端 E2E

修改：

```text
frontend/e2e/auth.spec.ts
```

适配登录：

- 登录页应显示验证码图片。
- 登录 helper 在点击登录前填写验证码。

注意：E2E 无法知道真实验证码答案时有两种方案：

方案 A：测试环境关闭验证码。

```env
AUTH_CAPTCHA_ENABLED=false
```

方案 B：后端 test/dev 暴露测试专用验证码。

不推荐在生产代码返回验证码答案。

建议采用方案 A：

- E2E 环境可以关闭验证码。
- 前端仍验证验证码 UI 存在。
- 后端单元测试覆盖验证码校验逻辑。

### 7.3 必须执行

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

如 E2E 环境需要关闭验证码，请在 Playwright 启动后端的环境中加入：

```env
AUTH_CAPTCHA_ENABLED=false
```

## 8. 验收标准

功能验收：

- 登录页显示验证码图片。
- 点击验证码图片或刷新按钮可刷新。
- 不输入验证码无法登录。
- 输入错误验证码无法登录。
- 验证码过期后无法登录。
- 正确验证码 + 正确账号密码可以登录。
- 登录失败后验证码会刷新。
- 登录成功后进入 Dashboard。

安全验收：

- 验证码答案不返回前端。
- 验证码答案不进入日志。
- 验证码一次性使用。
- 验证码有 TTL。
- 验证码有最大尝试次数。
- `/api/auth/captcha` 无需登录即可访问。
- 其他受保护 API 不受影响。

回归验收：

- Auth refresh token 不受影响。
- JWT filter 不受影响。
- Chat SSE 不受影响。
- Model Gateway 不受影响。
- 前端 13 个 E2E 测试通过。

## 9. 建议输出格式

完成后按以下格式输出：

1. 新增 / 修改文件清单
2. 后端验证码实现说明
3. 登录接口变更说明
4. 前端登录页变更说明
5. 配置项说明
6. 安全策略说明
7. 后端测试结果
8. 前端 typecheck / build / E2E 结果
9. 手动验证结果
10. 已知限制
11. 是否可以继续进入下一阶段

---

# Claude 执行提示词

请根据项目中的文档执行 Milestone 31。

文档路径：

```text
docs/milestone-31-login-captcha-security.md
```

执行要求：

1. 先完整阅读该文档，再检查当前 backend / frontend 代码结构。
2. 本阶段只实现登录验证码，不要重写 Auth / JWT / Security 架构。
3. 不要接短信、邮箱、第三方验证码服务。
4. 不要引入 Redis 作为强依赖。
5. 不要改变 ApiResponse、BizException、ErrorCode 的既有风格。
6. 不要使用 Lombok。
7. 不要把验证码答案返回给前端。
8. 不要把验证码答案写入日志。
9. 不要破坏已有登录、refresh token、JWT filter、受保护 API。
10. 前端只改登录验证码相关 UI 与 auth API/store，保持当前中文界面风格。
11. 如果测试环境需要关闭验证码，请通过配置完成，不要在生产代码里写测试后门。
12. 如果发现现有测试因验证码失败，优先更新测试 helper，不要删除断言。

需要实现：

1. 后端验证码配置 `CaptchaProperties`。
2. 后端验证码 DTO `CaptchaResponse`。
3. 后端验证码服务 `CaptchaService`：生成、图片 Base64、内存存储、TTL、最大尝试次数、一次性校验。
4. 后端验证码接口 `GET /api/auth/captcha`。
5. `LoginRequest` 增加 `captchaId`、`captchaCode`。
6. `AuthApplicationService.login()` 在账号密码校验前校验验证码。
7. `SecurityConfig` 放行 `/api/auth/captcha`。
8. `application.yml` 增加 `app.auth.captcha` 配置。
9. `ErrorCode` 增加验证码相关错误码。
10. 前端 `auth/api.ts` 增加 `getCaptcha()` 和验证码类型。
11. 前端 `auth/store.ts` 透传验证码字段。
12. 前端 `LoginPage.vue` 增加验证码输入、图片、刷新交互、失败后刷新。
13. 后端测试覆盖验证码接口、缺失/错误/过期/一次性使用等场景。
14. 前端 E2E 登录测试适配验证码。

完成后必须执行：

```bash
cd backend
mvn test

cd ../frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

完成后按文档第 9 节格式输出报告。

现在开始实现，不要只给计划。
