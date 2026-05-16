# Alpha / Beta Trial Plan

## 1. Alpha Trial — Internal Validation

### Goals
- 验证 Demo 主链路（Login → Project → Knowledge → Chat → Task → Observability）的可理解性和稳定性
- 发现阻塞性问题（P0/P1）
- 验证 Demo 文档（walkthrough、feedback template）的有效性
- 验证 UI 风格（Dark Tech Console）的用户接受度

### Participants
- **人数**: 5-10
- **画像**: 内部开发者、技术负责人、产品同学
- **不需要**: 外部用户、真实业务方

### Duration
- **周期**: 1-2 周
- **每个用户**: 30 分钟试用 + 10 分钟反馈
- **频次**: 每个用户 1-2 次

### Environment
- 本地开发环境 (`localhost:8080` + `localhost:5173`)
- 或单机 Docker Compose 演示环境
- **MOCK Provider** 模式（不要求真实模型）

### Process
1. **Preparation** (演示者):
   - 运行 `bash scripts/demo-seed-data.sh` 初始化数据
   - 运行 `bash scripts/demo-smoke-test.sh` 验证环境
   - 准备 [Demo Walkthrough](demo-walkthrough.md)（5 分钟快速版）
   - 准备 [User Feedback Template](user-feedback-template.md)

2. **Trial Session** (30 min):
   - 5 min: 平台介绍（不深入技术细节）
   - 15 min: 用户按 walkthrough 自行操作
   - 10 min: 收集反馈 + 填写模板

3. **Post-Session** (演示者):
   - 去敏反馈内容
   - 创建 GitHub Issue（`user_trial_feedback` 模板）
   - 按 [Triage Guide](user-trial-triage-guide.md) 分流
   - 记录到试用日志

### Pass Criteria
- [ ] ≥ 80% 用户能独立完成 Demo 主链路（6 个步骤中至少 5 个）
- [ ] 无未解决的 P0 / P1 问题
- [ ] 所有 P2 有明确的排期计划
- [ ] 用户整体满意度 ≥ 3/5（UI Rating 均值）
- [ ] ≥ 50% 用户表示愿意继续使用

### Block Criteria
- [ ] 存在 P0 安全漏洞（密钥泄露、认证绕过）
- [ ] 核心链路不可用（Chat SSE / Task Execute 完全中断）
- [ ] ≥ 50% 用户无法完成主链路
- [ ] 用户整体满意度 < 2/5

### Output
- 每个用户的试用反馈 Issue
- Alpha 总结报告（P0/P1/P2 统计、关键洞察、改进清单）
- 更新的 [CHANGELOG.md](../CHANGELOG.md)（如有修复）

---

## 2. Beta Trial — External Early Adopters

### Goals
- 验证真实模型配置下的 Chat / Task / PR Review 体验
- 验证 GitHub OAuth 集成
- 验证部署稳定性（单机 Docker Compose）
- 收集真实场景下的功能需求和模型质量反馈

### Participants
- **人数**: 20-30
- **画像**: 外部早期用户、真实业务团队（小规模）、有 GitHub 使用习惯的开发者
- **要求**: 愿意提供反馈、有真实代码项目或知识管理需求

### Duration
- **周期**: 3-4 周
- **每个用户**: 至少 2 次使用（首次上手 + 深度使用）
- **频次**: 灵活，建议每周至少 1 次

### Environment
- 单机 Docker Compose 演示环境
- 或用户自行部署（提供 [Production Deployment Runbook](production-deployment-runbook.md)）
- **Real Provider** 模式（用户自行配置 API Key）
- **GitHub OAuth** 可选配置

### Process
1. **Onboarding** (Day 0):
   - 发送部署文档和环境变量模板
   - 提供 Demo Walkthrough 和登录信息
   - 明确反馈提交方式（Issue Template）

2. **Trial Period** (Weeks 1-3):
   - 用户自行使用，按自己场景操作
   - 每周 check-in（Slack/邮件/GitHub Discussion）
   - 收集 Issue 和反馈

3. **Close-out** (Week 4):
   - 收集最终反馈
   - 汇总 Issue 和反馈分类
   - Beta 总结报告

### Pass Criteria
- [ ] 至少 3 个真实场景完成端到端试用
- [ ] 模型成本可解释（用户了解每次调用的 token 消耗）
- [ ] GitHub OAuth / PR Review 无高危安全问题
- [ ] 无未解决的 P0
- [ ] ≥ 30% 用户表示愿意继续使用或推荐

### Block Criteria
- [ ] 模型成本失控（超出预期 3x 以上）
- [ ] GitHub OAuth 存在安全漏洞
- [ ] 部署成功率 < 50%
- [ ] 核心反馈显示产品价值不匹配

### Output
- Beta 反馈汇总报告（分类统计、优先级分布）
- 模型质量评估报告（准确性、延迟、成本）
- 更新的 Roadmap（基于 Beta 反馈）
- v1.1 Release Notes

---

## 3. Trial Log Template

每个试用 Session 记录：

```
Session ID: TRIAL-YYYYMMDD-NN
Date:
User Role:
Environment: local / docker / prod
Provider: MOCK / OPENAI / CLAUDE / etc.
GitHub OAuth: configured / not-configured

Completed Path:
- [ ] Login
- [ ] Dashboard
- [ ] Project
- [ ] Knowledge / RAG
- [ ] Chat
- [ ] Task
- [ ] Model Gateway
- [ ] Observability
- [ ] GitHub (if configured)

Blocked At:
Key Feedback:
P0/P1 Issues:
P2 Issues:
Follow-up:
```

## 4. Post-Trial Retrospective

每次试用周期结束后：

1. 汇总所有 Issue 和反馈
2. 按 [Feedback Taxonomy](product-feedback-taxonomy.md) 分类
3. 统计 P0/P1/P2/P3 分布
4. 识别 Top 3 改进项
5. 更新 Roadmap 优先级
6. 输出总结报告
7. 决定是否进入下一阶段
