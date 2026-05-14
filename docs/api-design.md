# AI Coding Platform API 设计

## 1. 设计目标

本文档定义 AI Coding Platform 的 REST API 契约，覆盖认证、项目、成员、GitHub 仓库、Agent、AI Chat、AI 任务、知识库、审计、通知和管理后台接口。

API 设计目标：

- 为 Vue 前端和 Spring Boot 后端提供稳定接口契约。
- 统一认证、响应结构、分页、错误码和权限语义。
- 支持项目级数据隔离和 RBAC 权限控制。
- 支持 AI 流式输出、异步任务、GitHub 集成和 RAG 检索。
- 为后续生成 OpenAPI 文档和前端 TypeScript Client 预留结构。

## 2. 通用约定

### 2.1 Base URL

```text
/api
```

示例：

```text
GET /api/projects
```

### 2.2 认证方式

除登录、OAuth 回调、邮箱验证码等公开接口外，其他接口均需要携带 JWT。

请求头：

```http
Authorization: Bearer <access_token>
```

### 2.3 内容类型

默认请求与响应：

```http
Content-Type: application/json
```

文件上传：

```http
Content-Type: multipart/form-data
```

SSE 流式输出：

```http
Accept: text/event-stream
```

### 2.4 时间格式

所有时间字段使用 ISO-8601 字符串：

```text
2026-05-12T22:30:00.000-07:00
```

### 2.5 ID 类型

后端 ID 使用 `BIGINT`，前端为避免精度丢失，API 中统一以字符串返回。

示例：

```json
{
  "id": "1845123456789012345"
}
```

## 3. 统一响应结构

### 3.1 成功响应

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "f7f2e6f0b1c84a88",
  "timestamp": "2026-05-12T22:30:00.000-07:00"
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | string | 业务响应码 |
| message | string | 响应消息 |
| data | any | 响应数据 |
| traceId | string | 请求追踪 ID |
| timestamp | string | 响应时间 |

### 3.2 分页响应

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "records": [],
    "page": 1,
    "pageSize": 20,
    "total": 100,
    "hasNext": true
  },
  "traceId": "f7f2e6f0b1c84a88",
  "timestamp": "2026-05-12T22:30:00.000-07:00"
}
```

分页参数：

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| page | number | 1 | 页码，从 1 开始 |
| pageSize | number | 20 | 每页数量，最大 100 |
| sort | string | createTime,desc | 排序字段 |

### 3.3 错误响应

```json
{
  "code": "PROJECT_ACCESS_DENIED",
  "message": "No permission to access this project.",
  "details": {
    "projectId": "1845123456789012345"
  },
  "traceId": "f7f2e6f0b1c84a88",
  "timestamp": "2026-05-12T22:30:00.000-07:00"
}
```

### 3.4 通用错误码

| HTTP 状态 | code | 说明 |
| --- | --- | --- |
| 400 | BAD_REQUEST | 请求参数错误 |
| 401 | UNAUTHORIZED | 未登录或 Token 无效 |
| 403 | FORBIDDEN | 无操作权限 |
| 403 | PROJECT_ACCESS_DENIED | 无项目访问权限 |
| 404 | NOT_FOUND | 资源不存在 |
| 409 | CONFLICT | 资源冲突 |
| 422 | VALIDATION_ERROR | 参数校验失败 |
| 429 | RATE_LIMITED | 请求过于频繁 |
| 500 | INTERNAL_ERROR | 系统内部错误 |
| 502 | AI_PROVIDER_ERROR | 模型供应商调用失败 |
| 504 | AI_PROVIDER_TIMEOUT | 模型供应商调用超时 |

## 4. 权限约定

### 4.1 平台角色

| 角色 | 说明 |
| --- | --- |
| ADMIN | 平台管理员 |
| USER | 普通用户 |

### 4.2 项目角色

| 角色 | 说明 |
| --- | --- |
| OWNER | 项目负责人 |
| MAINTAINER | 维护者 |
| DEVELOPER | 开发成员 |
| VIEWER | 只读成员 |

### 4.3 权限标记

接口权限在文档中使用以下格式：

```text
Auth: required
Platform Role: ADMIN
Project Role: OWNER+
```

项目角色等级：

```text
OWNER > MAINTAINER > DEVELOPER > VIEWER
```

## 5. Auth API

### 5.1 账号密码登录

```http
POST /api/auth/login
```

Auth: public

请求：

```json
{
  "account": "admin@example.com",
  "password": "password123"
}
```

响应：

```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "jwt-refresh-token",
  "expiresIn": 7200,
  "user": {
    "id": "1",
    "username": "admin",
    "email": "admin@example.com",
    "avatar": null,
    "roles": ["ADMIN"]
  }
}
```

### 5.2 退出登录

```http
POST /api/auth/logout
```

Auth: required

响应：

```json
true
```

### 5.3 刷新 Token

```http
POST /api/auth/refresh
```

Auth: public

请求：

```json
{
  "refreshToken": "jwt-refresh-token"
}
```

响应：

```json
{
  "accessToken": "new-jwt-access-token",
  "refreshToken": "new-jwt-refresh-token",
  "expiresIn": 7200
}
```

### 5.4 当前用户

```http
GET /api/auth/me
```

Auth: required

响应：

```json
{
  "id": "1",
  "username": "admin",
  "email": "admin@example.com",
  "avatar": null,
  "githubBound": true,
  "roles": ["ADMIN"],
  "permissions": ["project:create", "agent:manage"],
  "tokenUsage": 102400
}
```

### 5.5 GitHub OAuth 授权地址

```http
GET /api/oauth/github/authorize
```

Auth: required

响应：

```json
{
  "authorizeUrl": "https://github.com/login/oauth/authorize?client_id=xxx&state=xxx"
}
```

### 5.6 GitHub OAuth 回调

```http
GET /api/oauth/github/callback?code=xxx&state=xxx
```

Auth: required

响应：

```json
{
  "bound": true,
  "githubLogin": "octocat",
  "avatarUrl": "https://github.com/images/error/octocat_happy.gif"
}
```

## 6. User API

### 6.1 用户列表

```http
GET /api/users?page=1&pageSize=20&keyword=tom&status=ENABLED
```

Auth: required  
Platform Role: ADMIN

响应：

```json
{
  "records": [
    {
      "id": "1",
      "username": "tom",
      "email": "tom@example.com",
      "avatar": null,
      "status": "ENABLED",
      "githubLogin": "tom",
      "tokenUsage": 12000,
      "createTime": "2026-05-12T22:30:00.000-07:00"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1,
  "hasNext": false
}
```

### 6.2 更新用户状态

```http
PUT /api/users/{userId}/status
```

Auth: required  
Platform Role: ADMIN

请求：

```json
{
  "status": "DISABLED"
}
```

响应：

```json
true
```

## 7. Project API

### 7.1 创建项目

```http
POST /api/projects
```

Auth: required

请求：

```json
{
  "name": "AI Coding Platform",
  "description": "Enterprise AI coding collaboration platform",
  "techStack": ["Java 17", "Spring Boot 3", "Vue 3"],
  "icon": "https://example.com/icon.png"
}
```

响应：

```json
{
  "id": "1845123456789012345",
  "name": "AI Coding Platform",
  "description": "Enterprise AI coding collaboration platform",
  "ownerId": "1",
  "techStack": ["Java 17", "Spring Boot 3", "Vue 3"],
  "status": "ACTIVE",
  "createTime": "2026-05-12T22:30:00.000-07:00"
}
```

### 7.2 项目列表

```http
GET /api/projects?page=1&pageSize=20&keyword=ai&status=ACTIVE
```

Auth: required

响应：

```json
{
  "records": [
    {
      "id": "1845123456789012345",
      "name": "AI Coding Platform",
      "description": "Enterprise AI coding collaboration platform",
      "icon": null,
      "ownerId": "1",
      "ownerName": "admin",
      "role": "OWNER",
      "repoBound": true,
      "memberCount": 5,
      "taskCount": 12,
      "status": "ACTIVE",
      "updateTime": "2026-05-12T22:30:00.000-07:00"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1,
  "hasNext": false
}
```

### 7.3 项目详情

```http
GET /api/projects/{projectId}
```

Auth: required  
Project Role: VIEWER+

响应：

```json
{
  "id": "1845123456789012345",
  "name": "AI Coding Platform",
  "description": "Enterprise AI coding collaboration platform",
  "icon": null,
  "ownerId": "1",
  "ownerName": "admin",
  "techStack": ["Java 17", "Spring Boot 3", "Vue 3"],
  "status": "ACTIVE",
  "repoUrl": "https://github.com/org/repo",
  "currentUserRole": "OWNER",
  "createTime": "2026-05-12T22:30:00.000-07:00",
  "updateTime": "2026-05-12T22:30:00.000-07:00"
}
```

### 7.4 更新项目

```http
PUT /api/projects/{projectId}
```

Auth: required  
Project Role: MAINTAINER+

请求：

```json
{
  "name": "AI Coding Platform",
  "description": "Updated description",
  "techStack": ["Java 17", "Spring Boot 3", "Vue 3", "RabbitMQ"],
  "icon": null
}
```

响应：

```json
true
```

### 7.5 归档项目

```http
DELETE /api/projects/{projectId}
```

Auth: required  
Project Role: OWNER

响应：

```json
true
```

### 7.6 项目概览

```http
GET /api/projects/{projectId}/overview
```

Auth: required  
Project Role: VIEWER+

响应：

```json
{
  "memberCount": 5,
  "taskCount": 20,
  "runningTaskCount": 2,
  "completedTaskCount": 12,
  "agentCount": 6,
  "documentCount": 14,
  "tokenUsage": 180000,
  "recentActivities": [
    {
      "type": "TASK_COMPLETED",
      "title": "Backend Agent completed task",
      "time": "2026-05-12T22:30:00.000-07:00"
    }
  ]
}
```

### 7.7 更新项目配置

```http
PUT /api/projects/{projectId}/config
```

Auth: required  
Project Role: OWNER

请求：

```json
{
  "defaultModelConfigId": "1001",
  "ragEnabled": true,
  "memoryEnabled": true,
  "maxTaskConcurrency": 3
}
```

响应：

```json
true
```

## 8. Project Member API

### 8.1 成员列表

```http
GET /api/projects/{projectId}/members?page=1&pageSize=20
```

Auth: required  
Project Role: VIEWER+

响应：

```json
{
  "records": [
    {
      "userId": "1",
      "username": "admin",
      "email": "admin@example.com",
      "avatar": null,
      "role": "OWNER",
      "status": "ACTIVE",
      "joinedTime": "2026-05-12T22:30:00.000-07:00"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1,
  "hasNext": false
}
```

### 8.2 邀请成员

```http
POST /api/projects/{projectId}/members
```

Auth: required  
Project Role: OWNER

请求：

```json
{
  "email": "dev@example.com",
  "role": "DEVELOPER"
}
```

响应：

```json
{
  "invitationId": "2001",
  "email": "dev@example.com",
  "role": "DEVELOPER",
  "status": "PENDING",
  "expireTime": "2026-05-19T22:30:00.000-07:00"
}
```

### 8.3 修改成员角色

```http
PUT /api/projects/{projectId}/members/{userId}/role
```

Auth: required  
Project Role: OWNER

请求：

```json
{
  "role": "MAINTAINER"
}
```

响应：

```json
true
```

### 8.4 移除成员

```http
DELETE /api/projects/{projectId}/members/{userId}
```

Auth: required  
Project Role: OWNER

响应：

```json
true
```

## 9. Repository API

### 9.1 GitHub 仓库列表

```http
GET /api/github/repositories?page=1&pageSize=30&keyword=platform
```

Auth: required

响应：

```json
{
  "records": [
    {
      "provider": "GITHUB",
      "fullName": "org/ai-coding-platform",
      "name": "ai-coding-platform",
      "description": "AI coding platform",
      "private": true,
      "defaultBranch": "main",
      "htmlUrl": "https://github.com/org/ai-coding-platform",
      "cloneUrl": "https://github.com/org/ai-coding-platform.git"
    }
  ],
  "page": 1,
  "pageSize": 30,
  "total": 1,
  "hasNext": false
}
```

### 9.2 绑定仓库

```http
POST /api/projects/{projectId}/repository/bind
```

Auth: required  
Project Role: OWNER

请求：

```json
{
  "provider": "GITHUB",
  "repoFullName": "org/ai-coding-platform",
  "repoUrl": "https://github.com/org/ai-coding-platform",
  "cloneUrl": "https://github.com/org/ai-coding-platform.git",
  "defaultBranch": "main"
}
```

响应：

```json
{
  "repositoryId": "3001",
  "status": "BOUND"
}
```

### 9.3 Clone 仓库

```http
POST /api/projects/{projectId}/repository/clone
```

Auth: required  
Project Role: MAINTAINER+

请求：

```json
{
  "branch": "main",
  "force": false
}
```

响应：

```json
{
  "operationId": "4001",
  "status": "PENDING"
}
```

### 9.4 Pull 最新代码

```http
POST /api/projects/{projectId}/repository/pull
```

Auth: required  
Project Role: MAINTAINER+

请求：

```json
{
  "branch": "main"
}
```

响应：

```json
{
  "operationId": "4002",
  "status": "PENDING"
}
```

### 9.5 分支列表

```http
GET /api/projects/{projectId}/repository/branches
```

Auth: required  
Project Role: VIEWER+

响应：

```json
[
  {
    "name": "main",
    "commitHash": "abc123",
    "protectedBranch": true,
    "lastSyncTime": "2026-05-12T22:30:00.000-07:00"
  }
]
```

### 9.6 查询 Diff

```http
GET /api/projects/{projectId}/repository/diff?base=main&head=feature/task-1
```

Auth: required  
Project Role: VIEWER+

响应：

```json
{
  "base": "main",
  "head": "feature/task-1",
  "files": [
    {
      "path": "src/main/java/App.java",
      "changeType": "MODIFIED",
      "additions": 10,
      "deletions": 2,
      "patch": "@@ -1,4 +1,5 @@"
    }
  ]
}
```

### 9.7 Commit 变更

```http
POST /api/projects/{projectId}/repository/commit
```

Auth: required  
Project Role: MAINTAINER+

请求：

```json
{
  "taskId": "5001",
  "branch": "feature/task-5001",
  "message": "feat: implement project task API"
}
```

响应：

```json
{
  "operationId": "4003",
  "commitHash": "abc123",
  "status": "SUCCESS"
}
```

### 9.8 Push 分支

```http
POST /api/projects/{projectId}/repository/push
```

Auth: required  
Project Role: MAINTAINER+

请求：

```json
{
  "branch": "feature/task-5001"
}
```

响应：

```json
{
  "operationId": "4004",
  "status": "PENDING"
}
```

### 9.9 创建 PR

```http
POST /api/projects/{projectId}/repository/pull-requests
```

Auth: required  
Project Role: MAINTAINER+

请求：

```json
{
  "title": "feat: implement project task API",
  "body": "Generated by Backend Agent.",
  "base": "main",
  "head": "feature/task-5001",
  "taskId": "5001"
}
```

响应：

```json
{
  "operationId": "4005",
  "prUrl": "https://github.com/org/repo/pull/12",
  "status": "SUCCESS"
}
```

## 10. Agent API

### 10.1 Agent 列表

```http
GET /api/agents?type=BACKEND&status=ENABLED
```

Auth: required

响应：

```json
[
  {
    "id": "6001",
    "name": "Backend Agent",
    "code": "backend-agent",
    "type": "BACKEND",
    "description": "Generate backend APIs and services.",
    "status": "ENABLED"
  }
]
```

### 10.2 创建 Agent

```http
POST /api/agents
```

Auth: required  
Platform Role: ADMIN

请求：

```json
{
  "name": "Backend Agent",
  "code": "backend-agent",
  "type": "BACKEND",
  "description": "Generate backend APIs and services.",
  "systemPrompt": "You are a backend coding agent.",
  "modelConfigId": "1001",
  "toolPolicy": {
    "allowedTools": ["file.read", "file.patch", "test.run"]
  }
}
```

响应：

```json
{
  "id": "6001",
  "versionId": "6101"
}
```

### 10.3 Agent 详情

```http
GET /api/agents/{agentId}
```

Auth: required

响应：

```json
{
  "id": "6001",
  "name": "Backend Agent",
  "code": "backend-agent",
  "type": "BACKEND",
  "description": "Generate backend APIs and services.",
  "status": "ENABLED",
  "latestVersion": {
    "id": "6101",
    "versionNo": "1.0.0",
    "modelConfigId": "1001",
    "status": "PUBLISHED"
  }
}
```

### 10.4 更新 Agent

```http
PUT /api/agents/{agentId}
```

Auth: required  
Platform Role: ADMIN

请求：

```json
{
  "name": "Backend Agent",
  "description": "Generate backend APIs, tests and docs.",
  "status": "ENABLED"
}
```

响应：

```json
true
```

### 10.5 项目启用 Agent

```http
POST /api/projects/{projectId}/agents/{agentId}/enable
```

Auth: required  
Project Role: OWNER

请求：

```json
{
  "agentVersionId": "6101",
  "modelConfigId": "1001",
  "config": {
    "temperature": 0.2
  }
}
```

响应：

```json
true
```

### 10.6 项目停用 Agent

```http
POST /api/projects/{projectId}/agents/{agentId}/disable
```

Auth: required  
Project Role: OWNER

响应：

```json
true
```

## 11. Task API

### 11.1 创建任务

```http
POST /api/projects/{projectId}/tasks
```

Auth: required  
Project Role: DEVELOPER+

请求：

```json
{
  "title": "生成项目管理接口",
  "description": "根据需求生成 Project Controller、Service 和 Mapper。",
  "taskType": "CODING",
  "agentId": "6001",
  "priority": "HIGH",
  "branch": "main",
  "sourceType": "MANUAL",
  "sourceId": null
}
```

响应：

```json
{
  "id": "5001",
  "projectId": "1845123456789012345",
  "title": "生成项目管理接口",
  "taskType": "CODING",
  "status": "PENDING",
  "priority": "HIGH",
  "agentId": "6001",
  "createTime": "2026-05-12T22:30:00.000-07:00"
}
```

### 11.2 任务列表

```http
GET /api/projects/{projectId}/tasks?page=1&pageSize=20&status=RUNNING&taskType=CODING
```

Auth: required  
Project Role: VIEWER+

响应：

```json
{
  "records": [
    {
      "id": "5001",
      "title": "生成项目管理接口",
      "taskType": "CODING",
      "agentName": "Backend Agent",
      "creatorName": "admin",
      "status": "RUNNING",
      "priority": "HIGH",
      "createTime": "2026-05-12T22:30:00.000-07:00",
      "startTime": "2026-05-12T22:31:00.000-07:00"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1,
  "hasNext": false
}
```

### 11.3 任务详情

```http
GET /api/tasks/{taskId}
```

Auth: required  
Project Role: VIEWER+

响应：

```json
{
  "id": "5001",
  "projectId": "1845123456789012345",
  "title": "生成项目管理接口",
  "description": "根据需求生成 Project Controller、Service 和 Mapper。",
  "taskType": "CODING",
  "agentId": "6001",
  "agentName": "Backend Agent",
  "creatorId": "1",
  "creatorName": "admin",
  "status": "RUNNING",
  "priority": "HIGH",
  "branch": "main",
  "retryCount": 0,
  "createTime": "2026-05-12T22:30:00.000-07:00"
}
```

### 11.4 启动任务

```http
POST /api/tasks/{taskId}/start
```

Auth: required  
Project Role: DEVELOPER+

响应：

```json
{
  "taskId": "5001",
  "status": "RUNNING"
}
```

### 11.5 取消任务

```http
POST /api/tasks/{taskId}/cancel
```

Auth: required  
Project Role: MAINTAINER+

请求：

```json
{
  "reason": "User canceled."
}
```

响应：

```json
{
  "taskId": "5001",
  "status": "CANCELED"
}
```

### 11.6 重试任务

```http
POST /api/tasks/{taskId}/retry
```

Auth: required  
Project Role: DEVELOPER+

响应：

```json
{
  "taskId": "5001",
  "status": "PENDING",
  "retryCount": 1
}
```

### 11.7 任务日志

```http
GET /api/tasks/{taskId}/logs?cursor=&limit=100
```

Auth: required  
Project Role: VIEWER+

响应：

```json
{
  "records": [
    {
      "id": "7001",
      "level": "INFO",
      "stage": "CONTEXT_BUILD",
      "message": "Loaded project context.",
      "createTime": "2026-05-12T22:30:00.000-07:00"
    }
  ],
  "nextCursor": "7001",
  "hasNext": false
}
```

### 11.8 任务产物

```http
GET /api/tasks/{taskId}/artifacts
```

Auth: required  
Project Role: VIEWER+

响应：

```json
[
  {
    "id": "8001",
    "artifactType": "PATCH",
    "name": "project-api.patch",
    "content": "diff --git a/...",
    "fileUrl": null,
    "createTime": "2026-05-12T22:30:00.000-07:00"
  }
]
```

## 12. Chat API

### 12.1 创建会话

```http
POST /api/projects/{projectId}/chat/sessions
```

Auth: required  
Project Role: DEVELOPER+

请求：

```json
{
  "title": "项目初始化讨论",
  "sessionType": "PROJECT"
}
```

响应：

```json
{
  "id": "9001",
  "projectId": "1845123456789012345",
  "title": "项目初始化讨论",
  "sessionType": "PROJECT",
  "status": "ACTIVE",
  "createTime": "2026-05-12T22:30:00.000-07:00"
}
```

### 12.2 会话列表

```http
GET /api/projects/{projectId}/chat/sessions?page=1&pageSize=20
```

Auth: required  
Project Role: VIEWER+

响应：

```json
{
  "records": [
    {
      "id": "9001",
      "title": "项目初始化讨论",
      "sessionType": "PROJECT",
      "lastMessage": "好的，我来分析项目结构。",
      "lastMessageTime": "2026-05-12T22:30:00.000-07:00",
      "status": "ACTIVE"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1,
  "hasNext": false
}
```

### 12.3 消息列表

```http
GET /api/chat/sessions/{sessionId}/messages?cursor=&limit=50
```

Auth: required  
Project Role: VIEWER+

响应：

```json
{
  "records": [
    {
      "id": "9101",
      "senderType": "USER",
      "senderId": "1",
      "senderName": "admin",
      "agentId": null,
      "messageType": "MARKDOWN",
      "content": "帮我分析项目结构",
      "status": "COMPLETED",
      "tokenUsage": 0,
      "references": [],
      "createTime": "2026-05-12T22:30:00.000-07:00"
    }
  ],
  "nextCursor": "9101",
  "hasNext": false
}
```

### 12.4 发送消息

```http
POST /api/chat/sessions/{sessionId}/messages
```

Auth: required  
Project Role: DEVELOPER+

请求：

```json
{
  "content": "请帮我生成项目管理模块接口。",
  "agentIds": ["6001"],
  "context": {
    "filePaths": ["src/main/java/com/example/project/Project.java"],
    "taskId": null
  },
  "stream": true
}
```

响应：

```json
{
  "userMessageId": "9101",
  "assistantMessageId": "9102",
  "streamUrl": "/api/chat/sessions/9001/stream?messageId=9102"
}
```

### 12.5 SSE 消息流

```http
GET /api/chat/sessions/{sessionId}/stream?messageId=9102
```

Auth: required  
Project Role: VIEWER+

SSE 事件：

```text
event: token
data: {"messageId":"9102","content":"好的，"}

event: reference
data: {"messageId":"9102","references":[{"type":"CODE","filePath":"src/main/java/App.java"}]}

event: done
data: {"messageId":"9102","status":"COMPLETED","tokenUsage":1200}

event: error
data: {"messageId":"9102","code":"AI_PROVIDER_TIMEOUT","message":"Model timeout"}
```

## 13. Knowledge API

### 13.1 上传文档

```http
POST /api/projects/{projectId}/knowledge/documents
```

Auth: required  
Project Role: DEVELOPER+

请求：

```http
multipart/form-data
file=<binary>
sourceType=UPLOAD
```

响应：

```json
{
  "id": "10001",
  "fileName": "requirement.md",
  "fileType": "MARKDOWN",
  "parseStatus": "PENDING",
  "createTime": "2026-05-12T22:30:00.000-07:00"
}
```

### 13.2 文档列表

```http
GET /api/projects/{projectId}/knowledge/documents?page=1&pageSize=20&parseStatus=INDEXED
```

Auth: required  
Project Role: VIEWER+

响应：

```json
{
  "records": [
    {
      "id": "10001",
      "fileName": "requirement.md",
      "fileType": "MARKDOWN",
      "sourceType": "UPLOAD",
      "parseStatus": "INDEXED",
      "chunkCount": 20,
      "createTime": "2026-05-12T22:30:00.000-07:00"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1,
  "hasNext": false
}
```

### 13.3 删除文档

```http
DELETE /api/knowledge/documents/{documentId}
```

Auth: required  
Project Role: MAINTAINER+

响应：

```json
true
```

### 13.4 重新索引

```http
POST /api/knowledge/documents/{documentId}/reindex
```

Auth: required  
Project Role: MAINTAINER+

响应：

```json
{
  "jobId": "11001",
  "status": "PENDING"
}
```

### 13.5 知识库检索

```http
POST /api/projects/{projectId}/knowledge/search
```

Auth: required  
Project Role: VIEWER+

请求：

```json
{
  "query": "项目权限如何设计？",
  "searchType": "HYBRID",
  "limit": 10,
  "filters": {
    "fileType": ["MARKDOWN", "CODE"],
    "filePathPrefix": "docs/"
  }
}
```

响应：

```json
[
  {
    "chunkId": "12001",
    "documentId": "10001",
    "title": "权限设计",
    "content": "项目级权限采用 Project Role...",
    "score": 0.89321,
    "filePath": "docs/system-architecture.md",
    "startLine": 120,
    "endLine": 138
  }
]
```

## 14. Audit API

### 14.1 平台审计日志

```http
GET /api/audit/logs?page=1&pageSize=20&action=PROJECT_CREATE&userId=1
```

Auth: required  
Platform Role: ADMIN

响应：

```json
{
  "records": [
    {
      "id": "13001",
      "projectId": "1845123456789012345",
      "userId": "1",
      "username": "admin",
      "action": "PROJECT_CREATE",
      "targetType": "PROJECT",
      "targetId": "1845123456789012345",
      "result": "SUCCESS",
      "createTime": "2026-05-12T22:30:00.000-07:00"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1,
  "hasNext": false
}
```

### 14.2 项目审计日志

```http
GET /api/projects/{projectId}/audit/logs?page=1&pageSize=20
```

Auth: required  
Project Role: OWNER

响应：

```json
{
  "records": [
    {
      "id": "13001",
      "action": "TASK_CREATE",
      "targetType": "AI_TASK",
      "targetId": "5001",
      "userId": "1",
      "username": "admin",
      "result": "SUCCESS",
      "createTime": "2026-05-12T22:30:00.000-07:00"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1,
  "hasNext": false
}
```

## 15. Notification API

### 15.1 通知列表

```http
GET /api/notifications?page=1&pageSize=20&readStatus=UNREAD
```

Auth: required

响应：

```json
{
  "records": [
    {
      "id": "14001",
      "projectId": "1845123456789012345",
      "type": "TASK",
      "title": "任务已完成",
      "content": "Backend Agent 已完成任务。",
      "linkUrl": "/projects/1845123456789012345/tasks/5001",
      "readStatus": "UNREAD",
      "createTime": "2026-05-12T22:30:00.000-07:00"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1,
  "hasNext": false
}
```

### 15.2 标记已读

```http
POST /api/notifications/{notificationId}/read
```

Auth: required

响应：

```json
true
```

## 16. Admin API

### 16.1 模型配置列表

```http
GET /api/admin/model-configs?provider=OPENAI&modelType=CHAT
```

Auth: required  
Platform Role: ADMIN

响应：

```json
[
  {
    "id": "1001",
    "provider": "OPENAI",
    "modelName": "gpt-4.1",
    "modelType": "CHAT",
    "apiBase": "https://api.openai.com/v1",
    "status": "ENABLED",
    "createTime": "2026-05-12T22:30:00.000-07:00"
  }
]
```

### 16.2 创建模型配置

```http
POST /api/admin/model-configs
```

Auth: required  
Platform Role: ADMIN

请求：

```json
{
  "provider": "OPENAI",
  "modelName": "gpt-4.1",
  "modelType": "CHAT",
  "apiBase": "https://api.openai.com/v1",
  "apiKey": "sk-***",
  "defaultParams": {
    "temperature": 0.2,
    "maxTokens": 4096
  }
}
```

响应：

```json
{
  "id": "1001"
}
```

### 16.3 Token 用量统计

```http
GET /api/admin/usage?scope=PROJECT&projectId=1845123456789012345&startDate=2026-05-01&endDate=2026-05-12
```

Auth: required  
Platform Role: ADMIN

响应：

```json
{
  "scope": "PROJECT",
  "promptTokens": 100000,
  "completionTokens": 80000,
  "totalTokens": 180000,
  "cost": "12.34000000",
  "items": [
    {
      "date": "2026-05-12",
      "totalTokens": 20000,
      "cost": "1.23000000"
    }
  ]
}
```

## 17. WebSocket 事件设计

WebSocket 地址：

```text
/ws
```

认证：

- 建连时通过 Query Token 或子协议传递 JWT。
- 推荐：`/ws?token=<access_token>`。

### 17.1 服务端事件

任务日志事件：

```json
{
  "event": "task.log",
  "projectId": "1845123456789012345",
  "taskId": "5001",
  "payload": {
    "level": "INFO",
    "stage": "CODING",
    "message": "Generated patch."
  }
}
```

任务状态事件：

```json
{
  "event": "task.status",
  "projectId": "1845123456789012345",
  "taskId": "5001",
  "payload": {
    "fromStatus": "RUNNING",
    "toStatus": "REVIEWING"
  }
}
```

AI Token 事件：

```json
{
  "event": "chat.token",
  "projectId": "1845123456789012345",
  "sessionId": "9001",
  "messageId": "9102",
  "payload": {
    "content": "好的，"
  }
}
```

通知事件：

```json
{
  "event": "notification.created",
  "payload": {
    "id": "14001",
    "title": "任务已完成"
  }
}
```

## 18. 幂等与并发约定

### 18.1 幂等请求头

对创建任务、Commit、Push、PR 等可能重复提交的接口，前端建议携带：

```http
Idempotency-Key: <uuid>
```

后端应在同一用户、同一接口、同一幂等键下返回同一结果。

### 18.2 乐观锁

项目配置、Agent 配置等更新接口后续可增加 `version` 字段：

```json
{
  "version": 3
}
```

版本不匹配返回：

```json
{
  "code": "CONFLICT",
  "message": "Resource version conflict."
}
```

## 19. API 优先级

### 19.1 P0 第一阶段

- Auth API。
- Project API。
- Project Member API。
- Repository API：仓库列表、绑定、Clone、分支列表。
- Agent API：列表、详情、项目启用。
- Task API：创建、列表、详情、启动、日志、产物。
- Chat API：会话、消息、SSE。

### 19.2 P1 第二阶段

- Knowledge API。
- Repository API：Commit、Push、PR。
- Admin API：模型配置、用量统计。
- Audit API。
- Notification API。

### 19.3 P2 第三阶段

- AI PR Review API。
- 多 Agent 工作流 API。
- DevOps 部署 API。
- MCP 插件 API。
- 成本预算与限额 API。

## 20. 后续待细化

- 是否输出完整 OpenAPI 3.0 YAML。
- 是否生成前端 TypeScript Client。
- 是否将 SSE 与 WebSocket 二选一作为第一阶段唯一流式方案。
- Git 写操作是否引入审批 API，例如 `/api/approvals`。
- 多 Agent 工作流是否独立为 Workflow API。
- 文件树、代码预览、Patch 应用是否需要独立 Code Workspace API。

