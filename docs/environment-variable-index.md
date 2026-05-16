# Environment Variable Index

All environment variables organized by category, with security levels.

**Security Levels:**
- **Public** — Safe to commit (non-sensitive defaults)
- **Internal** — Safe for internal dev, not for public
- **Secret** — Never commit, must override in production
- **CHANGE_ME** — Must be replaced before production use

---

## 1. Database

| Variable | Required | Default | Example | Security | Source |
|----------|----------|---------|---------|----------|--------|
| `DB_URL` | Yes | `jdbc:mysql://127.0.0.1:3307/ai_coding_platform?...` | — | Internal | `.env.example` |
| `DB_USERNAME` | Yes | `root` | — | Internal | `.env.example` |
| `DB_PASSWORD` | Yes | `platform123` | — | Secret / CHANGE_ME in prod | `.env.example` |
| `MYSQL_DATABASE` | No | `ai_coding_platform` | — | Internal | `.env.production.example` |
| `MYSQL_HOST_PORT` | No | `3317` | — | Internal | `.env.production.example` |

## 2. JWT

| Variable | Required | Default | Example | Security | Source |
|----------|----------|---------|---------|----------|--------|
| `JWT_SECRET` | Yes | (dev placeholder) | `openssl rand -base64 32` | **Secret** — CHANGE_ME in prod | `.env.example` |

Requirements: minimum 256 bits (32 characters). Generate: `openssl rand -base64 32`.

## 3. Spring / JVM

| Variable | Required | Default | Example | Security | Source |
|----------|----------|---------|---------|----------|--------|
| `SPRING_PROFILES_ACTIVE` | No | (empty) | `prod` | Internal | `.env.example` |
| `JAVA_OPTS` | No | `-Xmx512m` | `-Xms256m -Xmx768m -XX:+UseG1GC` | Internal | `.env.example` |

## 4. Model Gateway

| Variable | Required | Default | Example | Security | Source |
|----------|----------|---------|---------|----------|--------|
| `MODEL_GATEWAY_PROVIDER` | Yes | `MOCK` | `MOCK` / `OPENAI` / `CLAUDE` / `DEEPSEEK` / `QWEN` / `GEMINI` | Internal | `.env.example` |
| `MODEL_GATEWAY_FALLBACK_ENABLED` | No | `true` | `true` / `false` | Internal | `.env.production.example` |
| `MODEL_GATEWAY_TIMEOUT_MS` | No | `30000` | `60000` | Internal | `.env.example` |
| `MODEL_GATEWAY_RETRY_TIMES` | No | `1` | `2` | Internal | `.env.example` |
| `MODEL_GATEWAY_PROMPT_SAFETY_ENABLED` | No | `true` | `true` / `false` | Internal | `.env.production.example` |

## 5. Provider API Keys

All disabled by default. Set `*_ENABLED=true` and provide `*_API_KEY` to activate.

### OpenAI

| Variable | Required | Default | Example | Security | Source |
|----------|----------|---------|---------|----------|--------|
| `OPENAI_ENABLED` | No | `false` | `true` | Internal | `.env.example` |
| `OPENAI_BASE_URL` | No | `https://api.openai.com/v1` | — | Internal | `.env.production.example` |
| `OPENAI_API_KEY` | If enabled | (empty) | `sk-...` | **Secret** — never commit | `.env.example` |
| `OPENAI_MODEL` | No | `gpt-4.1-mini` | `gpt-4o` | Internal | `.env.example` |

### Claude (Anthropic)

| Variable | Required | Default | Example | Security | Source |
|----------|----------|---------|---------|----------|--------|
| `CLAUDE_ENABLED` | No | `false` | `true` | Internal | `.env.example` |
| `CLAUDE_BASE_URL` | No | `https://api.anthropic.com` | — | Internal | `.env.production.example` |
| `CLAUDE_API_KEY` | If enabled | (empty) | `sk-ant-...` | **Secret** — never commit | `.env.example` |
| `CLAUDE_MODEL` | No | `claude-3-5-sonnet-latest` | `claude-opus-4-7` | Internal | `.env.example` |

### DeepSeek

| Variable | Required | Default | Example | Security | Source |
|----------|----------|---------|---------|----------|--------|
| `DEEPSEEK_ENABLED` | No | `false` | `true` | Internal | `.env.example` |
| `DEEPSEEK_BASE_URL` | No | `https://api.deepseek.com/v1` | — | Internal | `.env.production.example` |
| `DEEPSEEK_API_KEY` | If enabled | (empty) | `sk-...` | **Secret** — never commit | `.env.example` |
| `DEEPSEEK_MODEL` | No | `deepseek-chat` | `deepseek-reasoner` | Internal | `.env.example` |

### Qwen (Tongyi Qianwen)

| Variable | Required | Default | Example | Security | Source |
|----------|----------|---------|---------|----------|--------|
| `QWEN_ENABLED` | No | `false` | `true` | Internal | `.env.example` |
| `QWEN_BASE_URL` | No | `https://dashscope.aliyuncs.com/compatible-mode/v1` | — | Internal | `.env.production.example` |
| `QWEN_API_KEY` | If enabled | (empty) | `sk-...` | **Secret** — never commit | `.env.example` |
| `QWEN_MODEL` | No | `qwen-plus` | `qwen-max` | Internal | `.env.example` |

### Gemini (Google)

| Variable | Required | Default | Example | Security | Source |
|----------|----------|---------|---------|----------|--------|
| `GEMINI_ENABLED` | No | `false` | `true` | Internal | `.env.example` |
| `GEMINI_BASE_URL` | No | `https://generativelanguage.googleapis.com/v1beta/openai` | — | Internal | `.env.production.example` |
| `GEMINI_API_KEY` | If enabled | (empty) | `AIza...` | **Secret** — never commit | `.env.example` |
| `GEMINI_MODEL` | No | `gemini-2.5-flash` | `gemini-2.5-pro` | Internal | `.env.example` |

## 6. GitHub OAuth

| Variable | Required | Default | Example | Security | Source |
|----------|----------|---------|---------|----------|--------|
| `GITHUB_CLIENT_ID` | If OAuth enabled | (empty) | `Ov23li...` | **Secret** — never commit | `.env.example` |
| `GITHUB_CLIENT_SECRET` | If OAuth enabled | (empty) | `ghs_...` | **Secret** — never commit | `.env.example` |
| `GITHUB_REDIRECT_URI` | If OAuth enabled | `http://localhost:8080/api/github/oauth/callback` | `https://example.com/api/github/oauth/callback` | Internal | `.env.example` |
| `GITHUB_OAUTH_SCOPES` | No | `repo,read:user,user:email` | — | Internal | `.env.example` |

## 7. Frontend

| Variable | Required | Default | Example | Security | Source |
|----------|----------|---------|---------|----------|--------|
| `VITE_API_BASE_URL` | No | `http://localhost:8080` | `""` (Docker) | Internal | `frontend/.env.example` |
| `VITE_APP_NAME` | No | `AI Coding Platform` | — | Public | `frontend/.env.example` |

## 8. Docker / Production

| Variable | Required | Default | Example | Security | Source |
|----------|----------|---------|---------|----------|--------|
| `COMPOSE_PROJECT_NAME` | No | `ai-coding-platform-prod` | — | Internal | `.env.production.example` |
| `NGINX_HTTP_PORT` | No | `80` | — | Internal | `.env.production.example` |
| `NGINX_HTTPS_PORT` | No | `443` | — | Internal | `.env.production.example` |
| `BACKEND_IMAGE` | No | (built locally) | `ghcr.io/.../backend:latest` | Internal | `.env.production.example` |
| `FRONTEND_IMAGE` | No | (built locally) | `ghcr.io/.../frontend:latest` | Internal | `.env.production.example` |

## 9. Domain / CORS

| Variable | Required | Default | Example | Security | Source |
|----------|----------|---------|---------|----------|--------|
| `APP_DOMAIN` | Production only | `example.com` | `platform.example.com` | Internal | `.env.production.example` |
| `APP_BASE_URL` | Production only | `https://example.com` | — | Internal | `.env.production.example` |
| `APP_CORS_ALLOWED_ORIGINS` | Production only | `https://example.com` | `https://a.com,https://b.com` | Internal | `.env.production.example` |

## 10. RabbitMQ

| Variable | Required | Default | Example | Security | Source |
|----------|----------|---------|---------|----------|--------|
| `RABBITMQ_DEFAULT_USER` | No | `platform` | — | Internal | `.env.production.example` |
| `RABBITMQ_DEFAULT_PASS` | No | (dev default) | — | Secret / CHANGE_ME in prod | `.env.production.example` |

## 11. SSL Certificates (HTTPS)

| Variable | Required | Default | Example | Security | Source |
|----------|----------|---------|---------|----------|--------|
| `NGINX_SSL_CERT_PATH` | HTTPS only | (empty) | `/etc/letsencrypt/live/example.com/fullchain.pem` | Internal | `.env.production.example` |
| `NGINX_SSL_KEY_PATH` | HTTPS only | (empty) | `/etc/letsencrypt/live/example.com/privkey.pem` | **Secret** — never commit | `.env.production.example` |

## 12. Smoke Test Config

| Variable | Required | Default | Example | Security | Source |
|----------|----------|---------|---------|----------|--------|
| `TEST_MODEL_PROVIDER` | No | (empty) | `OPENAI` | Internal | `.env.production.example` |
| `TEST_MODEL_NAME` | No | (empty) | `gpt-4.1-mini` | Internal | `.env.production.example` |
| `TEST_GITHUB_REPO_FULL_NAME` | No | (empty) | `owner/repo` | Internal | `.env.production.example` |
| `TEST_GITHUB_PR_NUMBER` | No | (empty) | `1` | Internal | `.env.production.example` |

---

## Files NOT to Commit

These files contain or may contain secrets and are in `.gitignore`:

- `.env` — Local environment variables
- `.env.production` — Production environment variables
- `*.pem`, `*.key`, `*.crt` — SSL certificates and keys
- `diagnostics/` — Diagnostic output directory
