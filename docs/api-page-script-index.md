# API / Page / Script Index

## 1. API Index

### Auth
| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/api/auth/login` | Public | Login with email + password |
| POST | `/api/auth/refresh` | Public (refresh token) | Refresh access token |
| GET | `/api/auth/me` | Authenticated | Get current user profile |

### Project
| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/api/projects` | DEVELOPER+ | Create project |
| GET | `/api/projects` | Authenticated | List user's projects |
| GET | `/api/projects/{id}` | Project member | Get project detail |
| PUT | `/api/projects/{id}` | MAINTAINER+ | Update project |
| DELETE | `/api/projects/{id}` | OWNER | Delete project |
| GET | `/api/projects/{id}/overview` | Project member | Project overview stats |

### Member
| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/api/projects/{id}/members` | MAINTAINER+ | Add member |
| GET | `/api/projects/{id}/members` | Project member | List members |
| PUT | `/api/projects/{id}/members/{userId}` | MAINTAINER+ | Update member role |
| DELETE | `/api/projects/{id}/members/{userId}` | MAINTAINER+ | Remove member |

### Repository
| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/api/projects/{id}/repository/bind` | DEVELOPER+ | Bind GitHub repo |
| POST | `/api/projects/{id}/repository/clone` | DEVELOPER+ | Clone bound repo |
| POST | `/api/projects/{id}/repository/pull` | DEVELOPER+ | Pull latest |
| GET | `/api/projects/{id}/repository/branches` | Project member | List branches |
| GET | `/api/projects/{id}/repository/diff` | Project member | Get diff between branches |

### Agent
| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/api/agents` | ADMIN | Create agent |
| GET | `/api/agents` | Authenticated | List agents |
| GET | `/api/agents/{id}` | Authenticated | Get agent detail |
| PUT | `/api/agents/{id}` | ADMIN | Update agent |
| DELETE | `/api/agents/{id}` | ADMIN | Delete agent |

### Task
| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/api/projects/{id}/tasks` | DEVELOPER+ | Create task |
| GET | `/api/projects/{id}/tasks` | Project member | List tasks |
| GET | `/api/tasks/{id}` | Project member | Get task detail |
| POST | `/api/tasks/{id}/execute` | DEVELOPER+ | Execute task |
| POST | `/api/tasks/{id}/cancel` | DEVELOPER+ | Cancel task |
| POST | `/api/tasks/{id}/retry` | DEVELOPER+ | Retry failed task |
| GET | `/api/tasks/{id}/logs` | Project member | Get task logs |
| GET | `/api/tasks/{id}/artifacts` | Project member | Get task artifacts |
| GET | `/api/tasks/{id}/executions` | Project member | Get execution history |
| GET | `/api/agent-executions/{id}` | Project member | Get execution detail |
| GET | `/api/agent-executions/{id}/model-logs` | Project member | Get execution model logs |

### Chat
| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/api/projects/{id}/chat/sessions` | DEVELOPER+ | Create chat session |
| GET | `/api/projects/{id}/chat/sessions` | Project member | List chat sessions |
| POST | `/api/chat/sessions/{id}/messages` | DEVELOPER+ | Send message |
| GET | `/api/chat/sessions/{id}/messages` | Project member | Get messages |
| GET | `/api/chat/sessions/{id}/stream` | Project member | SSE stream (real-time token emission) |

### RAG / Knowledge
| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/api/projects/{id}/knowledge-bases` | DEVELOPER+ | Create knowledge base |
| GET | `/api/projects/{id}/knowledge-bases` | Project member | List knowledge bases |
| PUT | `/api/projects/{id}/knowledge-bases/{id}` | DEVELOPER+ | Update KB |
| DELETE | `/api/projects/{id}/knowledge-bases/{id}` | MAINTAINER+ | Delete KB |
| POST | `/api/projects/{id}/knowledge-documents` | DEVELOPER+ | Upload document |
| GET | `/api/knowledge-documents/{id}` | Project member | Get document |
| DELETE | `/api/knowledge-documents/{id}` | DEVELOPER+ | Delete document |
| GET | `/api/knowledge-documents/{id}/chunks` | Project member | List document chunks |
| POST | `/api/projects/{id}/rag/search` | Project member | RAG semantic search |

### Model Gateway
| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | `/api/model-gateway/providers` | ADMIN | List configured providers |
| POST | `/api/model-gateway/test-connection` | ADMIN | Test provider connection |

### GitHub
| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | `/api/github/oauth/authorize` | Authenticated | Get OAuth authorization URL |
| GET | `/api/github/oauth/callback` | Public | OAuth callback |
| GET | `/api/github/repos` | Authenticated | List user's GitHub repos |
| POST | `/api/projects/{id}/pr-reviews` | MAINTAINER+ | Create PR review |

### Observability
| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | `/api/observability/overview` | ADMIN | System overview metrics |
| GET | `/api/projects/{id}/observability/overview` | ADMIN | Project-level overview |
| GET | `/api/observability/model-usage/summary` | ADMIN | Model usage summary |
| GET | `/api/projects/{id}/observability/model-usage/summary` | ADMIN | Project model usage |
| GET | `/api/projects/{id}/observability/model-usage/daily` | ADMIN | Daily model usage |

### Audit
| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | `/api/audit/logs` | ADMIN | List all audit logs |
| GET | `/api/projects/{id}/audit/logs` | ADMIN | Project audit logs |

## 2. Frontend Page Index

| Route | Vue Component | Auth Required | Description |
|-------|---------------|---------------|-------------|
| `/public` | PublicHomePage | No | Product landing page, feature showcase |
| `/login` | LoginPage | No | Email + password login |
| `/dashboard` | DashboardPage | Yes | System metrics overview |
| `/projects` | ProjectListPage | Yes | Project list with create dialog |
| `/projects/:id` | ProjectDetailPage | Yes | Project tabs: Overview/Tasks/Chat/Knowledge/Repository/Members |
| `/projects/:id/tasks` | TaskListPage | Yes | Task CRUD with execute dialog |
| `/projects/:id/tasks/:taskId` | TaskDetailPage | Yes | Task details: logs, artifacts, executions |
| `/projects/:id/chat` | ChatPage | Yes | Chat SSE with sessions |
| `/projects/:id/knowledge` | KnowledgeBasePage | Yes | KB management, RAG search |
| `/projects/:id/repository` | RepositoryPanel | Yes | Branch list, diff viewer |
| `/agents` | AgentListPage | Yes | Agent management |
| `/model-gateway` | ModelConfigPage | Yes (Admin) | Provider config, connection test |
| `/github` | GithubIntegrationPage | Yes | GitHub OAuth, PR review |
| `/observability` | ObservabilityPage | Yes (Admin) | System dashboards |

## 3. Script Index

### Development
| Script | Purpose |
|--------|---------|
| `scripts/dev-reset-db.sh` | Reset local database |
| `scripts/dev-seed-demo-data.sh` | Seed demo data for local dev |

### Demo
| Script | Purpose |
|--------|---------|
| `scripts/demo-seed-data.sh` | Initialize demo data (idempotent) |
| `scripts/demo-smoke-test.sh` | Run demo smoke test (curl-based) |
| `scripts/demo-reset-data.sh` | Clean up demo data |

### Testing
| Script | Purpose |
|--------|---------|
| `scripts/run-backend-checks.sh` | Backend: compile → test → package |
| `scripts/run-frontend-checks.sh` | Frontend: install → typecheck → build → E2E |
| `scripts/run-all-checks.sh` | Full check suite |
| `scripts/backend-unified-smoke-test.sh` | Backend API smoke test (curl) |

### Docker
| Script | Purpose |
|--------|---------|
| `scripts/docker-build-local.sh` | Build Docker images locally |
| `scripts/docker-smoke-test.sh` | Smoke test Docker deployment |

### Production
| Script | Purpose |
|--------|---------|
| `scripts/prod-deploy.sh` | Deploy production stack |
| `scripts/prod-smoke-test.sh` | Smoke test production deployment |
| `scripts/prod-backup-mysql.sh` | Backup MySQL database |
| `scripts/prod-restore-mysql.sh` | Restore from backup |
| `scripts/prod-logs.sh` | View production logs |

### Security
| Script | Purpose |
|--------|---------|
| `scripts/prod-security-check.sh` | Production security scan |
| `scripts/prod-health-check.sh` | Health check all services |
| `scripts/prod-log-scan.sh` | Scan logs for errors/warnings |
| `scripts/validate-model-provider.sh` | Validate model provider configuration |
| `scripts/validate-github-oauth-config.sh` | Validate GitHub OAuth configuration |

### Diagnostics
| Script | Purpose |
|--------|---------|
| `scripts/prod-alert-check.sh` | Check alert conditions |
| `scripts/prod-diagnostics.sh` | Run system diagnostics |

### Release
| Script | Purpose |
|--------|---------|
| `scripts/release-checklist.sh` | Release readiness check |
| `scripts/release-demo-check.sh` | Pre-release demo verification |
| `scripts/frontend-bundle-check.sh` | Bundle size budget check |
| `scripts/collect-trial-report.sh` | Aggregate trial feedback reports |
| `scripts/prod-external-services-smoke-test.sh` | Test external service connectivity |
