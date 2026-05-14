# AI Coding Platform 项目目录说明

## 1. 总体结构

```text
ai-coding-platform/
  backend/                 # Spring Boot 后端工程
  frontend/                # Vue 3 前端工程
  docs/                    # 产品、架构、数据库、API 文档
  infra/                   # 本地基础设施配置
  deploy/                  # 部署配置
  scripts/                 # 开发、构建、运维脚本
  .github/                 # GitHub Actions 工作流
```

## 2. 后端目录

```text
backend/
  src/main/java/com/aicoding/platform/
    common/                # 通用响应、异常、分页、工具
    security/              # Spring Security、JWT、权限上下文
    auth/                  # 用户、登录、OAuth、平台角色权限
    project/               # 项目基础信息和项目配置
    member/                # 项目成员和项目角色
    repository/            # GitHub 仓库、Clone、Branch、Diff、PR
    task/                  # AI 任务、状态流转、日志、产物
    chat/                  # AI Chat、会话、消息、流式输出
    agent/                 # Agent 定义、版本、项目启用配置
    knowledge/             # 知识库文档、索引任务
    ai/                    # AI 编排、模型网关、Prompt、工具、RAG、Memory
    audit/                 # 审计日志、AI 调用日志、工具调用日志
    notification/          # 站内通知、任务提醒
  src/main/resources/
    mapper/                # MyBatis XML
    db/migration/          # Flyway/Liquibase 数据库迁移脚本
    static/                # 静态资源
    templates/             # 模板资源
  src/test/java/           # 后端测试
```

每个业务模块默认采用以下分层：

```text
module/
  controller/              # REST API Controller
  application/             # 应用服务，编排业务流程
  domain/                  # 领域模型、领域服务、枚举
  infrastructure/          # Mapper、外部 API、消息、存储适配
  dto/                     # Request、Response、Command、DTO
```

## 3. AI 核心目录

```text
backend/src/main/java/com/aicoding/platform/ai/
  orchestrator/            # Agent 调度中心
  model/                   # Model Gateway，多模型适配
  prompt/                  # Prompt Engine，模板和版本
  context/                 # Context Builder，上下文组装
  tool/                    # Tool Runtime，工具注册和权限
  rag/                     # RAG Engine，解析、向量化、检索
  memory/                  # Memory Service，会话和项目记忆
  coding/                  # Coding Engine，代码生成和修改
  review/                  # Review Engine，PR 和代码审查
```

## 4. 前端目录

```text
frontend/
  public/                  # 公共静态资源
  src/
    app/
      router/              # Vue Router
      store/               # Pinia 全局 Store
      layouts/             # 应用布局
      guards/              # 路由守卫
    shared/
      api/                 # HTTP Client 和通用 API 封装
      components/          # 通用组件
      composables/         # 通用组合式函数
      constants/           # 常量
      types/               # 通用 TypeScript 类型
      utils/               # 工具函数
    modules/
      auth/                # 登录、OAuth
      dashboard/           # 工作台
      project/             # 项目管理
      member/              # 项目成员
      repository/          # 仓库、分支、Diff、PR
      task/                # AI 任务
      chat/                # AI Chat
      agent/               # Agent 配置
      knowledge/           # 知识库
      admin/               # 管理后台
    styles/                # 全局样式
    assets/                # 图片、字体等资源
```

## 5. 基础设施目录

```text
infra/
  mysql/                   # MySQL 初始化和本地配置
  redis/                   # Redis 配置
  rabbitmq/                # RabbitMQ 配置
  minio/                   # 对象存储配置
  vector-db/               # 向量数据库配置
  nginx/                   # Nginx 配置
```

## 6. 部署目录

```text
deploy/
  docker/                  # Dockerfile、docker-compose 等
  kubernetes/              # K8s Deployment、Service、Ingress 等
```

## 7. 开发顺序建议

1. 搭建 `backend` Spring Boot 基础工程。
2. 搭建 `frontend` Vue 3 + TypeScript + Vite 工程。
3. 完成 `common`、`security`、`auth`、`project`、`member`。
4. 接入 `repository` 和 GitHub OAuth。
5. 完成 `agent`、`chat`、`task` 和基础 `ai` 执行链路。
6. 扩展 `knowledge`、`rag`、`memory`、`coding`、`review`。

