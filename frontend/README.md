# AI Coding Platform Frontend

Vue 3 + Vite + TypeScript 企业级控制台

## 技术栈

- Vue 3 (Composition API)
- TypeScript
- Vite
- Element Plus
- Pinia
- Vue Router
- Axios
- markdown-it + highlight.js

## 快速启动

### 1. 安装依赖

```bash
npm install
```

### 2. 配置环境变量

```bash
cp .env.example .env
```

默认配置：
```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_NAME=AI Coding Platform
```

### 3. 启动后端

```bash
cd ../backend
mvn spring-boot:run
```

### 4. 启动前端

```bash
npm run dev
```

访问 http://localhost:5173

### 5. 构建

```bash
npm run build
```

### 6. 类型检查

```bash
npm run typecheck
```

## 目录结构

```
src/
├── main.ts                    # 入口
├── App.vue                    # 根组件
├── styles/                    # 全局样式
├── app/
│   ├── router/index.ts        # 路由配置
│   ├── guards/authGuard.ts    # 路由守卫
│   └── layouts/
│       ├── BasicLayout.vue    # 主布局 (侧边栏 + 顶栏 + 内容)
│       └── AuthLayout.vue     # 认证布局
├── shared/
│   ├── api/
│   │   ├── client.ts          # Axios 客户端 (拦截器)
│   │   └── types.ts           # ApiResponse / PageResult 类型
│   ├── components/            # 共享组件
│   ├── composables/           # 共享 composables
│   ├── constants/             # 常量
│   ├── types/                 # 通用类型
│   └── utils/                 # 工具函数 (auth, format, sse)
└── modules/
    ├── auth/                  # 登录 / Auth Store
    ├── dashboard/             # 首页 Dashboard
    ├── project/               # 项目管理
    ├── task/                  # 任务管理
    ├── chat/                  # Chat 会话 (SSE)
    ├── agent/                 # Agent 管理
    ├── knowledge/             # 知识库管理
    └── admin/                 # Observability / 审计日志
```

## 开发账号

| 邮箱 | 密码 |
|------|------|
| admin@example.com | Admin@123456 |

## 路由说明

| 路径 | 页面 | 权限 |
|------|------|------|
| /login | 登录页 | 公开 |
| /dashboard | 系统概览 | 登录后 |
| /projects | 项目列表 | 登录后 |
| /projects/:id | 项目详情 | 登录后 |
| /projects/:id/tasks | 任务管理 | 登录后 |
| /projects/:id/tasks/:taskId | 任务详情 | 登录后 |
| /projects/:id/chat | Chat 会话 | 登录后 |
| /projects/:id/knowledge | 知识库 | 登录后 |
| /projects/:id/repository | 仓库管理 | 登录后 |
| /projects/:id/members | 成员管理 | 登录后 |
| /agents | Agent 列表 | 登录后 |
| /observability | 可观测性 (ADMIN) | 登录后 |

## 冒烟测试

详见 [docs/frontend-smoke-test-plan.md](../docs/frontend-smoke-test-plan.md)
