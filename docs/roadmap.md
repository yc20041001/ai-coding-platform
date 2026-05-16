# Roadmap

AI Coding Platform 产品路线图。

## v1.0 — Internal Alpha

**Status**: Completed (Milestone 1-30). Final delivery package delivered.

**目标**: 完成内部试用，验证核心链路可行性。

**用户**: 5-10 内部开发者、技术负责人。

**核心功能**:
- [x] Auth (JWT login, role-based access: ADMIN/DEVELOPER/VIEWER)
- [x] Project CRUD with status management
- [x] Knowledge Base + RAG (document upload, chunking, search)
- [x] Chat with SSE streaming + RAG references
- [x] Task creation + Agent execution (PENDING → RUNNING → COMPLETED/FAILED)
- [x] Model Gateway with Mock provider + fallback
- [x] Observability (audit logs, model usage, overview)
- [x] Member management (add/remove/role assignment)
- [x] Repository browsing
- [x] Real model provider support (OpenAI, Claude, DeepSeek, Qwen, Gemini) — configurable
- [x] GitHub OAuth + PR Review — configurable
- [x] Production Docker Compose deployment
- [x] Backend testing: 144 tests, 14 test classes
- [x] Frontend E2E: 13/13 passing (×2 stable)
- [x] Final delivery documentation package (7 index/report docs)
- [x] Bundle optimization (index.js: 1,039 KB → 8 KB)

**Final Delivery**: See [Final Delivery Report](final-delivery-report.md) and [Handoff Guide](project-handoff-guide.md).

## v1.1 — External Beta

**Status**: Ready for Beta (Milestone 20-25)

**目标**: 真实模型配置 + GitHub OAuth/PR Review 试点。

**用户**: 20-30 外部早期用户或真实业务团队。

**周期**: 3-4 周。

**核心功能**:
- [x] Production Docker Compose deployment (nginx + frontend + backend + mysql + redis + rabbitmq)
- [x] Real model provider support (OpenAI, Claude, DeepSeek, Qwen, Gemini) with API key management
- [x] GitHub OAuth + read-only PR Review
- [x] Production monitoring, alerting, security hardening
- [x] Demo data initialization and smoke testing
- [x] Dark tech console UI (DynamicWorkspace, FloatingDock, StatusPulse)
- [x] Production security scripts (health check, security check, log scan, alert check, diagnostics)
- [ ] User feedback loop (in progress — Milestone 25)
- [ ] Model cost optimization (first pass)
- [ ] GitHub PR Review quality improvements

**通过标准**:
- [ ] 至少 3 个真实场景完成端到端试用
- [ ] 模型成本可解释
- [ ] GitHub OAuth / PR Review 无高危安全问题
- [ ] 试用者愿意继续使用或二次试用
- [ ] 80% 反馈已 triage 并排入后续迭代

## v1.2 — Team Collaboration

**Status**: Planned

**目标**: 团队协作体验增强。

**计划功能**:
- [ ] Multi-member real-time Chat Session
- [ ] Task assignment and notification
- [ ] Project-level model provider configuration
- [ ] Enhanced audit logging with user activity timeline
- [ ] RAG knowledge base sharing between projects
- [ ] Code snippet save and share from Chat
- [ ] Model usage dashboard per project

**用户**: 30-50 团队用户。

## v2.0 — Production Platform

**Status**: Planned

**目标**: 商业化生产平台。

**计划功能**:
- [ ] Multi-tenancy with organization/team hierarchy
- [ ] Billing and subscription (per-seat, per-token options)
- [ ] SLA monitoring and reporting
- [ ] Compliance and data governance (GDPR, SOC 2)
- [ ] Advanced RBAC with custom roles
- [ ] Audit export and SIEM integration
- [ ] HA deployment (Kubernetes, multi-node)
- [ ] API rate limiting and quota management
- [ ] Webhook notifications
- [ ] SSO / SAML / OIDC
- [ ] On-premise deployment option

## Iteration Cadence

| Phase | Duration | Output |
|-------|----------|--------|
| Alpha Sprint | 2 weeks | Bug fixes + UX improvements |
| Beta Sprint | 3-4 weeks | Feature iteration + feedback integration |
| Release | Per milestone | CHANGELOG + Release Notes |

## How to Contribute to Roadmap

1. File a [Feature Request](https://github.com/yc20041001/ai-coding-platform/issues/new?template=feature_request.yml)
2. Join user trials and submit [Trial Feedback](https://github.com/yc20041001/ai-coding-platform/issues/new?template=user_trial_feedback.yml)
3. Attend weekly triage meetings
4. Review [CHANGELOG.md](../CHANGELOG.md) for latest changes

## Status Key

- [x] Completed
- [ ] Planned
- [~] In Progress
