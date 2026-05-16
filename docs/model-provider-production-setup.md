# Model Provider Production Setup

## 1. Overview

The AI Coding Platform supports multiple model providers through a unified Model Gateway. Each provider requires an API key and optional base URL configuration. When no real provider is configured, the system falls back to Mock mode automatically.

## 2. Supported Providers

| Provider | Provider Key | Supports Stream | OpenAI Compatible | Setup Doc |
|----------|-------------|-----------------|-------------------|-----------|
| Mock | `MOCK` | Yes | N/A | None (built-in) |
| OpenAI | `OPENAI` | Yes | Yes | [OpenAI API Keys](https://platform.openai.com/api-keys) |
| Claude (Anthropic) | `CLAUDE` | Yes | No (native API) | [Anthropic Console](https://console.anthropic.com/) |
| DeepSeek | `DEEPSEEK` | Yes | Yes | [DeepSeek API Keys](https://platform.deepseek.com/api_keys) |
| Qwen (Tongyi) | `QWEN` | Yes | Yes | [Alibaba Cloud](https://dashscope.console.aliyun.com/) |
| Gemini (Google) | `GEMINI` | Yes | Yes | [Google AI Studio](https://aistudio.google.com/) |

## 3. Environment Variables

All provider configuration is read from environment variables in `.env.production`:

```bash
# ---- Default Provider ----
MODEL_GATEWAY_PROVIDER=MOCK          # MOCK / OPENAI / CLAUDE / DEEPSEEK / QWEN / GEMINI
MODEL_GATEWAY_FALLBACK_ENABLED=true  # Fall back to Mock on provider failure
MODEL_GATEWAY_TIMEOUT_MS=30000       # Request timeout (ms)
MODEL_GATEWAY_RETRY_TIMES=1          # Retry count (timeout/network/rate-limit only)

# ---- OpenAI ----
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_API_KEY=sk-your-key-here
OPENAI_MODEL=gpt-4.1-mini

# ---- Claude (Anthropic) ----
CLAUDE_BASE_URL=https://api.anthropic.com
CLAUDE_API_KEY=sk-ant-your-key-here
CLAUDE_MODEL=claude-3-5-sonnet-latest

# ---- DeepSeek ----
DEEPSEEK_BASE_URL=https://api.deepseek.com/v1
DEEPSEEK_API_KEY=sk-your-key-here
DEEPSEEK_MODEL=deepseek-chat

# ---- Qwen (Tongyi Qianwen) ----
QWEN_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
QWEN_API_KEY=sk-your-key-here
QWEN_MODEL=qwen-plus

# ---- Gemini (Google) ----
GEMINI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai
GEMINI_API_KEY=your-key-here
GEMINI_MODEL=gemini-2.5-flash
```

### Variable Reference

| Variable | Required | Default | Notes |
|----------|----------|---------|-------|
| `MODEL_GATEWAY_PROVIDER` | No | `MOCK` | Default provider for all AI calls |
| `MODEL_GATEWAY_FALLBACK_ENABLED` | No | `true` | Auto-fallback to Mock on failure |
| `MODEL_GATEWAY_TIMEOUT_MS` | No | `30000` | Per-request timeout |
| `MODEL_GATEWAY_RETRY_TIMES` | No | `1` | Retries for timeout/network/rate-limit |
| `{PROVIDER}_ENABLED` | No | `false` | Enable a specific provider |
| `{PROVIDER}_API_KEY` | No | (empty) | API key for the provider |
| `{PROVIDER}_BASE_URL` | No | (see above) | Base URL (must include `/v1` for OpenAI-compatible) |
| `{PROVIDER}_MODEL` | No | (see above) | Default model name |

## 4. Configuring a Provider

### Step 1: Obtain an API Key

1. Go to the provider's API key management page.
2. Create a new API key.
3. Copy the key immediately (many providers only show it once).

### Step 2: Add to .env.production

```bash
# Example: enable DeepSeek as default
MODEL_GATEWAY_PROVIDER=DEEPSEEK
DEEPSEEK_ENABLED=true
DEEPSEEK_API_KEY=sk-your-real-key
DEEPSEEK_MODEL=deepseek-chat
```

### Step 3: Restart Backend

```bash
bash scripts/prod-deploy.sh restart backend
```

### Step 4: Verify Connection

```bash
bash scripts/validate-model-provider.sh
```

Or test via the UI: **Model Gateway → Test Connection**.

## 5. Connection Test

The Model Gateway page provides a **Test Connection** button for each provider. It sends a minimal "ping" prompt and verifies:

- API key is valid
- Base URL is reachable
- Model responds successfully
- Latency is measured

### Error Diagnosis

| Error | Meaning | Fix |
|-------|---------|-----|
| `AUTH_ERROR` / HTTP 401 | Invalid API key | Check `{PROVIDER}_API_KEY` |
| `AUTH_ERROR` / HTTP 403 | Insufficient permissions | Check API key quotas/limits |
| `TIMEOUT` | Provider not reachable | Check `{PROVIDER}_BASE_URL` and network |
| `NETWORK_ERROR` | DNS/connection failure | Check firewall and DNS |
| `RATE_LIMIT` / HTTP 429 | Too many requests | Wait and retry |
| `CONFIG_ERROR` | Provider not enabled | Set `{PROVIDER}_ENABLED=true` |

## 6. Chat SSE with Real Models

When a real provider is configured:
1. Chat messages stream tokens in real-time via SSE.
2. Model Gateway logs show the real provider and model name.
3. On failure, the system falls back to Mock (if `FALLBACK_ENABLED=true`).

### Verifying SSE

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123456"}' | \
  python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

# Create session and send message
# Then stream via: GET /api/chat/sessions/{sessionId}/stream?messageId={id}
```

## 7. Task Execute with Real Models

Tasks use the default provider configured in `MODEL_GATEWAY_PROVIDER`. Each execution:
1. Resolves the provider and model.
2. Calls the model via Model Gateway.
3. Logs usage in `ModelRequestLog`.
4. Updates the observability cost/usage panel.

## 8. Usage, Cost, and ModelRequestLog

Every model call (real or mock) is logged to `model_request_log` with:

- `provider` — MOCK / OPENAI / CLAUDE / DEEPSEEK / QWEN / GEMINI
- `model_name` — The resolved model name
- `success` — Whether the call succeeded
- `fallback_used` — Whether fallback to Mock was triggered
- `error_code` — Error type if failed
- `prompt_tokens`, `completion_tokens`, `total_tokens`
- `estimated_cost` — Cost estimate in USD
- `latency_ms` — Response time

View in **Observability → Model Usage**.

## 9. Security

- API keys are **never** returned in API responses.
- API keys are masked in logs: `sk-a1****bcde`.
- The frontend Model Config page shows only masked keys.
- Connection Test only verifies connectivity; keys are never persisted from the test UI.

## 10. Fallback Behavior

```
Request → Provider (real)
  ├─ Success → Return response
  ├─ Timeout/Network/RateLimit → Retry (up to RETRY_TIMES)
  │   ├─ Success → Return response
  │   └─ Fail → Fallback to Mock (if enabled)
  └─ Auth/Config/Other Error → Fallback to Mock (if enabled)
```

Fallback can be disabled per-request or globally via `MODEL_GATEWAY_FALLBACK_ENABLED=false`.

## 11. Validation Script

Run the automated validation:

```bash
bash scripts/validate-model-provider.sh
```

This script checks:
- Each configured provider's environment variables are consistent.
- Mock provider is available (built-in).
- Provides SKIP for unconfigured providers (not FAIL).
