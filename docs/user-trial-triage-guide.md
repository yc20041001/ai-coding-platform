# User Trial Triage Guide

反馈分流和优先级分派流程。

## 1. Overview

每次用户试用后，产生的反馈需要经过以下 8 步流程：

```
Collect → Mask → Classify → Prioritize → Reproduce → Schedule → Verify → Close
```

## 2. Step-by-Step

### Step 1: Collect（收集）

来源：
- [User Feedback Template](user-feedback-template.md)（.md）
- [User Trial Feedback Issue](https://github.com/yc20041001/ai-coding-platform/issues/new?template=user_trial_feedback.yml)
- 演示者现场记录
- 浏览器 Console / Network 截图

收集 Checklist：
- [ ] 反馈者角色和使用场景
- [ ] 完成路径（勾选）
- [ ] 卡住位置（具体步骤）
- [ ] 评分（UI / Model Quality / Performance）
- [ ] traceId（如有）
- [ ] 截图/录屏链接

### Step 2: Mask（去敏）

在反馈进入任何公共系统前，必须检查：

- [ ] 移除 API Key（`sk-*`、`ghp_*`、`Bearer *`）
- [ ] 移除密码、Token、Session Cookie
- [ ] 移除真实用户邮箱（除非用户同意）
- [ ] 移除截图中的敏感信息（个人信息、密钥）
- [ ] 日志脱敏：使用 `ModelSecretMaskingService.sanitizeForLog()` 或 `sed` 替换

**Security P0**: 如果发现反馈中包含真实密钥：
1. 立即通知密钥所有者
2. 轮换密钥
3. 清理所有副本（Issue、文档、日志）

### Step 3: Classify（分类）

使用 [Product Feedback Taxonomy](product-feedback-taxonomy.md) 将反馈归入：

| 一级分类 | 何时使用 |
|----------|---------|
| Bug | 功能与预期不一致 |
| UX / Usability | 能用但难以理解或操作 |
| Product Value | 功能价值判断、场景需求 |
| Model Quality | 模型输出、引用、延迟、成本 |
| Security / Compliance | 权限、密钥、审计 |
| Deployment / Operations | 安装、部署、运维 |

每个反馈可标记多个子类（如 "Auth / Permission + UX / Usability"）。

### Step 4: Prioritize（定级）

使用 P0-P3 优先级：

| 级别 | 标准 | 响应时间 | 示例 |
|------|------|---------|------|
| **P0** | 数据泄露、密钥泄露、登录绕过、生产宕机、数据破坏 | 立即 | API Key 出现在响应中 |
| **P1** | 核心链路中断、Chat SSE 大面积失败、Task 无法执行、登录频繁失败 | 24h 内 | RAG 搜索完全不可用 |
| **P2** | 可复现但有 workaround、UI 明显影响理解、模型输出一般但不阻塞 | 下一迭代 | 页面加载慢 |
| **P3** | 文案问题、小视觉问题、低频场景、长期增强建议 | 记录，批量处理 | 按钮文字不统一 |

### Step 5: Reproduce（复现）

收集复现所需信息：

| 信息 | 来源 | 重要性 |
|------|------|--------|
| 环境（local/docker/prod） | 反馈者 | Required |
| 账号角色 | 反馈者 | Required |
| 具体操作步骤 | 反馈者 | Required |
| traceId | 响应 Header / 错误信息 | Recommended |
| 截图/录屏 | 反馈者 | Recommended |
| 后端日志 | `prod-logs.sh` 或 `demo-diagnostics.sh` | Recommended |
| 数据库状态 | 仅开发/测试环境 | Optional |

无法复现的反馈标记为 `cannot-reproduce`，记录环境差异。

### Step 6: Schedule（排期）

基于优先级和 Roadmap 排期：

| 优先级 | 排期 |
|--------|------|
| P0 | 立即修复，hotfix |
| P1 | 当前迭代 / 本周内 |
| P2 | 下一迭代（v1.1 或 v1.2） |
| P3 | Backlog，批量处理 |

排期 Checklist：
- [ ] 关联 GitHub Issue
- [ ] 分配 Owner
- [ ] 更新 Roadmap 或 Release Notes

### Step 7: Verify（验收）

修复完成后：

- [ ] 自动化测试覆盖（如适用）
- [ ] 手动验证（按原始复现步骤）
- [ ] 回归测试（相关功能无退化）
- [ ] 更新 [CHANGELOG.md](../CHANGELOG.md)
- [ ] 反馈确认（可选 — 联系原始反馈者确认修复）

### Step 8: Close（关闭）

关闭条件：
- [ ] 所有 Verification 步骤通过
- [ ] Issue 关联到 Release
- [ ] Release Notes 包含修复说明
- [ ] 已知限制已记录（如不能完全修复）

## 3. Quick Reference

### Triage Labels

| GitHub Label | 含义 |
|-------------|------|
| `bug` | Bug 报告 |
| `enhancement` | 功能请求 |
| `trial-feedback` | 试用反馈 |
| `triage` | 待分流 |
| `P0` / `P1` / `P2` / `P3` | 优先级 |
| `cannot-reproduce` | 无法复现 |
| `wontfix` | 不修复 |
| `security` | 安全问题 |
| `ux` | 用户体验 |
| `documentation` | 文档相关 |

### Feedback → Issue Mapping

| 反馈来源 | 创建 Issue 模板 | 初始 Labels |
|----------|---------------|------------|
| 用户试用反馈表 | `user_trial_feedback.yml` | `trial-feedback`, `triage` |
| Bug 发现 | `bug_report.yml` | `bug`, `triage` |
| 功能建议 | `feature_request.yml` | `enhancement`, `triage` |
| 发布验证 | `release_checklist.yml` | `release` |

## 4. Weekly Triage Meeting

建议每周进行 15 分钟 triage：

1. 检查未分流的 Issues（`triage` label）
2. 分类 + 定级
3. 无法复现的标记 `cannot-reproduce`
4. 分配 P0/P1 到当前迭代
5. P2/P3 排入下一迭代或 Backlog
6. 更新 Roadmap
