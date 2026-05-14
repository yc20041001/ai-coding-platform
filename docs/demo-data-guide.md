# Demo Data Guide

## 概述

`scripts/dev-seed-demo-data.sh` 提供一套可重复生成的演示数据，用于：

- 本地演示
- 功能验证
- 新人上手体验

## 前置条件

1. 后端已启动在 `http://localhost:8080`
2. 数据库已通过 Flyway 完成迁移
3. `curl`, `python3` 可用

## 演示账号

| 角色 | 邮箱 | 密码 |
|------|------|------|
| Admin | admin@example.com | Admin@123456 |

## 演示数据内容

脚本按顺序创建以下数据（幂等，已存在则复用）：

| 序号 | 数据 | 名称 | 说明 |
|------|------|------|------|
| 1 | Demo Project | Demo Project | 包含 Java, Spring Boot, Vue 3, TypeScript 技术栈 |
| 2 | Knowledge Base | Demo Knowledge Base | chunkSize=300, chunkOverlap=30 |
| 3 | Document | Agent Orchestrator Guide | Markdown 文档，介绍编排器架构与 RAG 集成 |
| 4 | Chat Session | Demo Chat Session | PROJECT 类型会话 |
| 5 | Chat Message | "Hello! Please explain..." | 用户消息 + Mock 助手回复 |
| 6 | Task | Demo Task - Implement Greeting API | FEATURE 类型，MEDIUM 优先级 |
| 7 | Task Execution | 执行上述 Task | 使用 Mock Agent，状态 COMPLETED |

## 使用方法

### 初始化演示数据

```bash
# 默认连接 http://localhost:8080
bash scripts/dev-seed-demo-data.sh

# 自定义 Base URL
BASE_URL=http://localhost:8080 bash scripts/dev-seed-demo-data.sh
```

成功输出示例：

```
==========================================
  演示数据初始化完成
==========================================
  Project ID:       200001
  Knowledge Base:   300001
  Document:         400001
  Chat Session:     500001
  Task:             600001
==========================================
```

### 幂等性

脚本通过检查名称来复用已有数据（"Demo Project"、"Demo Knowledge Base"），不会重复创建。可以安全地多次运行。

## 演示路径建议

推荐演示流程：

1. **登录** → `http://localhost:5173/login`，使用 admin 账号
2. **Dashboard** → 查看项目概览
3. **Projects** → 点击 Demo Project 进入
4. **Tasks Tab** → 查看 Demo Task，点击"详情"查看 Logs / Artifacts / Executions
5. **Chat Tab** → 进入 Demo Chat Session，查看消息历史
6. **Knowledge Tab** → 查看 Demo Knowledge Base，执行 RAG 搜索 "Agent Orchestrator"
7. **Observability** → 查看 Overview 和 Audit Logs（需 Admin 权限）

## 清理演示数据

### 重置数据库

```bash
# 重置测试库（推荐）
bash scripts/dev-reset-db.sh --yes

# 重置开发库
bash scripts/dev-reset-db.sh --url "jdbc:mysql://127.0.0.1:3306/ai_coding_platform" --yes
```

**安全警告**：重置脚本只允许操作名称包含 `ai_coding_platform` 的本地数据库，且拒绝操作名称包含 `prod` 或 `production` 的库。

### 手动清理

如需手动删除 Demo 数据，在应用内操作：

1. 进入 Demo Project → 删除 Task
2. 进入 Demo Project → Knowledge Tab → 删除 Document → 删除 Knowledge Base
3. 进入 Demo Project → Chat Tab → 删除 Session
4. 删除 Demo Project

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `BASE_URL` | `http://localhost:8080` | 后端 API 地址 |
| `ADMIN_EMAIL` | `admin@example.com` | Admin 邮箱 |
| `ADMIN_PASSWORD` | `Admin@123456` | Admin 密码 |
