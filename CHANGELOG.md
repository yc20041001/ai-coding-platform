# Changelog

All notable changes to the AI Coding Platform.

## [Unreleased]

### Added
- Milestone 30: Final delivery package and project archive preparation
  - Final Delivery Report (completion status, module inventory, quality gates, known limitations)
  - Project Handoff Guide (Day 1 setup, testing, troubleshooting, releasing, feedback handling)
  - Documentation Index (~70 docs organized by 13 topics)
  - API / Page / Script Index (12 API groups, 14 page routes, 28 scripts)
  - Environment Variable Index (12 categories, all env vars with security levels)
  - Final Release Checklist (10-section pre-release verification)
  - Milestone 30 Validation Report Template
- Milestone 29: Backend test coverage enhancement and quality gates
  - Backend Test Matrix (14 test classes, 144 tests, module risk coverage)
  - Backend Testing Guide (technology stack, patterns, data rules, troubleshooting)
  - Backend Coverage Report Template
  - JWT Token Provider unit tests (16 tests)
  - Task State Machine unit tests (20 tests)
  - RAG Document Chunk Service unit tests (14 tests)
  - Model Gateway unit test expansions (Secret Masking +8, Pricing +7, PR Review +11)
  - TestDataFactory for unique test data generation
  - Backend test gate integrated into release-checklist.sh as blocking check
- Milestone 25: Feedback loop and product iteration management
  - GitHub Issue templates (bug report, feature request, user trial feedback, release checklist)
  - Pull request template with security verification checklist
  - Product feedback taxonomy (6 categories, 30+ subcategories)
  - User trial triage guide (8-step process: collect → mask → classify → prioritize → reproduce → schedule → verify → close)
  - Alpha/Beta trial plan (Alpha: 5-10 users, 1-2 weeks; Beta: 20-30 users, 3-4 weeks)
  - Roadmap (v1.0 Internal Alpha → v1.1 External Beta → v1.2 Team Collaboration → v2.0 Production Platform)
  - Release notes template (Keep a Changelog format)
  - Trial report collection script (`scripts/collect-trial-report.sh`)
  - Release readiness check script (`scripts/release-checklist.sh`)

### Changed
- Updated README with feedback, roadmap, and release documentation links
- Updated demo walkthrough and user feedback template with triage/issue links

## [v0.9.0] — Milestone 20-24

### Added
- Milestone 24: Productized demo environment with seed/reset/smoke scripts, demo walkthrough, user feedback template, acceptance checklist
- Milestone 23: Production monitoring, alerting, and security hardening
  - Health check, security check, log scan, alert check, and diagnostics scripts
  - Observability runbook, alerting rules, security hardening checklist, incident response runbook
- Milestone 22: Real model provider and GitHub OAuth production validation
  - Model provider setup documentation (OpenAI, Claude, DeepSeek, Qwen, Gemini)
  - GitHub OAuth production setup documentation
  - Validation scripts and external services smoke test
- Milestone 21: Cloud demo production deployment
  - Production Docker Compose (6 services: nginx, frontend, backend, mysql, redis, rabbitmq)
  - Nginx config with SSE support, Vue Router fallback, security headers
  - Spring production profile with configurable CORS
  - Deploy, backup, restore, logs, and smoke test scripts
- Milestone 20B: UI polish and consistency
  - DynamicWorkspace, FloatingDock, dark tech console theme
  - Unified status indicators, error states, loading states
- Milestone 20A: Frontend visual upgrade

### Changed
- CORS configuration supports comma-separated `APP_CORS_ALLOWED_ORIGINS`
- Frontend Shared Components reorganized for reusability

### Security
- API key masking in all responses and logs (`sk-****abcd` format)
- `ModelSecretMaskingService` sanitizes Bearer tokens and api_key patterns
- GitHub token encrypted at rest (`accessTokenEnc`), never returned in API
- PR Review prompt contains only PR metadata and diff — no tokens
- `.env.production` excluded from git tracking
- `diagnostics/` directory added to `.gitignore`
- All production scripts use `set -euo pipefail` and mask secrets in output

### Documentation
- Production deployment runbook
- Deployment guide
- Model provider and GitHub OAuth setup guides
- Observability, alerting, security, and incident response docs
- Demo data guide, walkthrough, and acceptance checklist
- User feedback template

### Known Limitations
- E2E test "should create a project" may timeout intermittently (UI timing)
- RAG search may need ~30s after document upload for indexing
- Chat session DELETE API not currently available

---

## Format

This changelog follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) conventions:

- **Added**: New features
- **Changed**: Changes to existing functionality
- **Fixed**: Bug fixes
- **Security**: Security improvements
- **Documentation**: Documentation changes
- **Known Limitations**: Documented limitations for this release
