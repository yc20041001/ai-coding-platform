# Production Deployment Config

## Files

| File | Purpose |
|------|---------|
| `docker-compose.prod.yml` | Production Compose: mysql, redis, rabbitmq, backend, frontend, nginx |
| `nginx.http.conf` | HTTP-only nginx config (use with Cloudflare proxy or local testing) |
| `nginx.https.conf.example` | HTTPS nginx config with Certbot integration |

## Quick Start

```bash
# From project root:
cp .env.production.example .env.production
# Edit .env.production — replace ALL CHANGE_ME values

bash scripts/prod-deploy.sh up --build
bash scripts/prod-smoke-test.sh http://localhost
```

## Architecture

```
Internet → nginx (:80/:443) → frontend (:80) — static files + Vue Router
                            → backend (:8080) — /api/* + SSE
                            
Internal only (no public ports):
  mysql (:3306), redis (:6379), rabbitmq (:5672)
```

## HTTPS Options

1. **Cloudflare Proxy**: Use `nginx.http.conf`, set Cloudflare to Full (strict), origin server only needs port 80.
2. **Certbot (Let's Encrypt)**: Use `nginx.https.conf.example`, mount certs as volumes.
3. **Self-signed (dev/test only)**: Generate dummy certs for local testing.

See `docs/production-deployment-runbook.md` for full details.
