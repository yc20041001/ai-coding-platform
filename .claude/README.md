# Claude Agent Workspace

本目录用于沉淀 AI Coding Platform 的 Claude Code / AI Agent 协作设计。

## 目录结构

```text
.claude/
  README.md
  ai-agent-design.md          # Agent 总体设计
  agents/                     # 单个 Agent 角色提示词
  workflows/                  # 多 Agent 协作流程
  policies/                   # 工具权限、安全、上下文策略
  templates/                  # 输出模板
```

## 使用顺序

1. 先阅读 `ai-agent-design.md` 理解整体 Agent 模型。
2. 根据任务类型选择 `agents/` 下对应 Agent。
3. 根据任务流程参考 `workflows/`。
4. 执行前检查 `policies/` 中的权限、安全和上下文规则。
5. 输出结果时使用 `templates/` 中的模板。

## Agent 选择建议

| 任务类型 | 推荐 Agent |
| --- | --- |
| 需求分析、验收标准 | Product Agent |
| 架构设计、模块拆分 | Architect Agent |
| Spring Boot 后端开发 | Backend Agent |
| Vue 3 前端开发 | Frontend Agent |
| 测试用例、自动化测试 | Test Agent |
| 代码审查、PR Review | Review Agent |
| CI/CD、Docker、K8s | DevOps Agent |
| 知识库、检索、Embedding | RAG Agent |

## 全局原则

- 所有 Agent 必须遵守 `policies/safety-policy.md`。
- 所有工具调用必须遵守 `policies/tool-permissions.md`。
- 所有上下文读取必须遵守 `policies/context-policy.md`。
- 涉及代码实现时必须遵守 `docs/development-guidelines.md`。
- 涉及 API 时必须遵守 `docs/api-design.md`。
- 涉及数据库时必须遵守 `docs/database-design.md`。

