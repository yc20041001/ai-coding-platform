# Product Feedback Taxonomy

反馈分类体系 — 所有用户反馈必须归入以下分类和子类。

## 1. Bug

定义：功能与预期不一致，或导致错误、崩溃、数据异常。

| 子类 | 说明 | 示例 |
|------|------|------|
| Backend API | API 返回错误码、500、数据不一致 | POST /api/tasks 返回 500 |
| Frontend UI | 页面渲染异常、JS 错误、交互失效 | 按钮点击无响应 |
| Auth / Permission | 登录、鉴权、角色权限异常 | Viewer 能看到 Admin 页面 |
| Chat SSE | 流式输出中断、断连、不完整 | SSE 30s 后断开 |
| RAG / Knowledge | 文档上传、分块、搜索异常 | 搜索返回空结果但有文档 |
| Task / Agent | 任务创建、执行、状态机异常 | COMPLETED 任务可以重新执行 |
| Model Gateway | 模型调用、回退、连接测试异常 | MOCK 连接测试失败 |
| GitHub / PR Review | OAuth、仓库浏览、PR 审查异常 | 回调 URL 不匹配 |
| Deployment / Docker | 容器启动、端口、网络异常 | nginx 502 Bad Gateway |
| Observability / Audit | 指标、日志、审计数据异常 | 审计日志缺失 |

## 2. UX / Usability

定义：功能能用，但用户难以理解、路径过长、反馈不清晰。

| 子类 | 说明 | 示例 |
|------|------|------|
| Navigation | 菜单、Tab、面包屑、路由问题 | 找不到 Observability 入口 |
| Empty / Loading / Error State | 空白页、无提示、错误信息不友好 | 空列表显示空白 |
| Form / Dialog / Drawer | 表单验证、对话框交互问题 | 必填字段无提示 |
| Table / Filter / Pagination | 表格排序、筛选、分页问题 | 筛选后分页未重置 |
| Visual Hierarchy | 信息层级、视觉重点不清晰 | 关键操作按钮不明显 |
| Onboarding | 首次使用引导不足 | 不知道第一步该做什么 |

## 3. Product Value

定义：用户认为功能价值高/低、场景不匹配、缺少关键能力。

| 子类 | 说明 | 示例 |
|------|------|------|
| Developer Workflow | 开发者日常工作流 | "希望 Chat 能直接生成可运行的代码" |
| Team Collaboration | 团队协作场景 | "希望多人同时看同一个 Chat Session" |
| AI Code Review | AI 辅助代码审查 | "PR Review 只给了概述，没有逐行建议" |
| RAG Knowledge Management | 知识库管理场景 | "希望支持 PDF 上传" |
| Project Management | 项目管理场景 | "希望 Task 能设置截止日期" |
| Model Provider Management | 模型管理场景 | "希望看到每个 provider 的成本对比" |

## 4. Model Quality

定义：模型输出质量、引用质量、成本、延迟、回退体验问题。

| 子类 | 说明 | 示例 |
|------|------|------|
| Answer Accuracy | 回答正确性 | "生成的代码有语法错误" |
| Hallucination | 虚构信息 | "引用了一个不存在的 API" |
| Citation Relevance | 引用相关性 | "RAG 引用和问题无关" |
| Prompt Quality | 提示词质量 | "系统提示词导致回答格式不对" |
| Latency | 响应延迟 | "Chat 回复等了 10 秒才开始流式" |
| Token Cost | Token 消耗 | "一次回答消耗了 5000 tokens" |
| Provider Failure | 供应商故障 | "Claude 返回 503，回退到 MOCK" |

## 5. Security / Compliance

定义：权限、密钥、日志、审计、数据暴露风险。

| 子类 | 说明 | 示例 |
|------|------|------|
| Auth Bypass | 认证绕过 | "不登录就能访问 /api/projects" |
| Token Leakage | Token 泄露 | "API 响应中包含明文 accessToken" |
| Secret Logging | 密钥记录 | "日志中打印了 API Key" |
| Over-Permission | 权限过大 | "Viewer 可以删除 Project" |
| Audit Missing | 审计缺失 | "删除操作没有审计记录" |
| Unsafe CORS | CORS 配置不安全 | "Access-Control-Allow-Origin: *" |

## 6. Deployment / Operations

定义：安装、启动、部署、监控、备份、升级相关问题。

| 子类 | 说明 | 示例 |
|------|------|------|
| Docker | Docker 镜像、容器问题 | "镜像构建失败，缺少 Dockerfile" |
| Nginx | Nginx 配置、代理问题 | "WebSocket 升级失败" |
| MySQL | 数据库连接、迁移、备份问题 | "Flyway 迁移校验和不匹配" |
| Environment Variables | 环境变量配置问题 | "JWT_SECRET 太短导致启动失败" |
| CI/CD | 持续集成/部署问题 | "GitHub Actions 构建超时" |
| Health Check | 健康检查问题 | "健康检查一直 failing 但服务正常" |
| Backup / Restore | 备份恢复问题 | "恢复后数据不完整" |

## Issue Template Mapping

反馈分类对应 GitHub Issue 模板：

| 分类 | Issue Template |
|------|---------------|
| Bug (全部子类) | `bug_report.yml` |
| UX / Usability | `bug_report.yml` (label: ux) 或 `feature_request.yml` |
| Product Value | `feature_request.yml` |
| Model Quality | `bug_report.yml` (area: Model Gateway) |
| Security / Compliance | `bug_report.yml` (severity: P0/P1) |
| Deployment / Operations | `bug_report.yml` (area: Deployment / Docker) |
| User Trial Feedback | `user_trial_feedback.yml` |
