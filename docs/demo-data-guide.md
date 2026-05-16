# Demo Data Guide

## 概述

Demo 数据脚本提供一套可重复生成的演示数据，用于：

- 本地演示
- 功能验证
- 新人上手体验
- 用户试用

## 快速开始

```bash
# 1. 确保后端已启动
cd backend && mvn spring-boot:run

# 2. 初始化 Demo 数据（幂等，可重复运行）
bash scripts/demo-seed-data.sh

# 3. 验证 Demo 环境
bash scripts/demo-smoke-test.sh

# 4. 浏览器打开 http://localhost:5173
#    登录：admin@example.com / Admin@123456

# 5. 按 Demo Walkthrough 体验
#    见 docs/demo-walkthrough.md
```

## 前置条件

1. 后端已启动在 `http://localhost:8080`
2. 数据库已通过 Flyway 完成迁移
3. `curl`, `python3` 可用

## 演示账号

| 角色 | 邮箱 | 密码 |
|------|------|------|
| Admin | admin@example.com | Admin@123456 |

## 演示数据内容

`scripts/demo-seed-data.sh` 按顺序创建以下数据（幂等，已存在则复用）：

| 序号 | 数据 | 名称 | 说明 |
|------|------|------|------|
| 1 | Demo Project | Demo AI Workspace | 包含 Java, Spring Boot, Vue 3, TypeScript, RAG, AI Agent 技术栈 |
| 2 | Knowledge Base | Product Knowledge Base | chunkSize=300, chunkOverlap=30 |
| 3 | Document 1 | AI Coding Platform Overview | 平台能力总览：Chat、RAG、Task、Model Gateway、GitHub、Observability |
| 4 | Document 2 | Agent Workflow Guide | Agent 编排器执行流程、任务状态机、集成点 |
| 5 | Document 3 | Repository Review Guide | GitHub PR Review 只读流程和安全说明 |
| 6 | Chat Session | Ask Product Knowledge | PROJECT 类型会话，预置 RAG 消息 |
| 7 | Chat Message | "Please summarize how..." | 用户消息 + Mock 助手回复，useRag=true |
| 8 | Task 1 | Generate architecture review summary | REVIEW 类型，MEDIUM 优先级，Agent 执行 |
| 9 | Task 2 | Implement health check endpoint | FEATURE 类型，HIGH 优先级 |

## 脚本说明

### demo-seed-data.sh

初始化 Demo 数据，幂等（通过名称检查复用已有数据）。

```bash
# 默认连接 http://localhost:8080
bash scripts/demo-seed-data.sh

# 自定义后端地址
BASE_URL=http://localhost:8080 bash scripts/demo-seed-data.sh

# 自定义 Demo 项目名
DEMO_PROJECT_NAME="My Demo" bash scripts/demo-seed-data.sh
```

输出格式：`[PASS]` / `[WARN]` / `[FAIL]` / `[SKIP]`。

### demo-smoke-test.sh

验证 Demo 环境是否就绪，检查 10 大类：
Frontend → Login → Auth/me → Project → Knowledge/RAG → Chat → Task → Model Gateway → Observability/Audit → Security

```bash
bash scripts/demo-smoke-test.sh
```

退出码 0 = 就绪，非 0 = 有问题。会报告 MOCK/Real Provider 状态。

### demo-reset-data.sh

清理 Demo 前缀数据。**必须传 `--yes`**，否则只显示帮助。

```bash
# 查看将删除什么（不执行）
bash scripts/demo-reset-data.sh

# 执行删除
bash scripts/demo-reset-data.sh --yes
```

安全保证：
- 只删除名称包含 "Demo" 或已知 Demo 名称前缀的数据
- 不执行 DROP DATABASE
- 不触碰非 Demo 数据
