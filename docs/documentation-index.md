# Documentation Index

All project documentation organized by topic. ~70 documents.

## Getting Started

| Document | Reader | Purpose |
|----------|--------|---------|
| [README.md](../README.md) | Everyone | Project overview, quick start, architecture |
| [Project Handoff Guide](project-handoff-guide.md) | New maintainers | Day 1 setup, testing, troubleshooting, releasing |
| [Final Delivery Report](final-delivery-report.md) | Stakeholders | Completion status, quality gates, known limitations |

## Architecture

| Document | Reader | Purpose |
|----------|--------|---------|
| [System Architecture](system-architecture.md) | Developers | Component architecture, data flow |
| [Project Structure](project-structure.md) | Developers | Directory layout, module organization |
| [Module Breakdown](module-breakdown.md) | Developers | Detailed module responsibilities |
| [API Design](api-design.md) | Backend devs | API conventions, response format |
| [Database Design](database-design.md) | Backend devs | Schema, indexes, Flyway migrations |
| [Development Guidelines](development-guidelines.md) | Developers | Coding standards, patterns |
| [Requirements](requirements.md) | PM/Devs | Original requirements doc |

## Backend

| Document | Reader | Purpose |
|----------|--------|---------|
| [Backend Testing Guide](backend-testing-guide.md) | Backend devs | Test patterns, profile, running tests |
| [Backend Test Matrix](backend-test-matrix.md) | QA/Devs | Module risk coverage, test counts |
| [Backend Coverage Report Template](backend-coverage-report-template.md) | QA | Coverage report format |

## Frontend

| Document | Reader | Purpose |
|----------|--------|---------|
| [Frontend Smoke Test Plan](frontend-smoke-test-plan.md) | QA | Manual smoke test checklist |
| [Frontend Performance Budget](frontend-performance-budget.md) | Frontend devs | Bundle size budgets |
| [Bundle Analysis Report](bundle-analysis-report.md) | Frontend devs | Before/After optimization metrics |
| [E2E Stability Guide](e2e-stability-guide.md) | QA | Selector priority, data isolation, waiting strategies |

## Testing (Combined)

| Document | Reader | Purpose |
|----------|--------|---------|
| [Testing Strategy](testing-strategy.md) | Everyone | 3-layer strategy, quality gates, test patterns |
| [Unified Backend Regression Test Plan](unified-backend-regression-test-plan.md) | QA | Backend regression test scope |

## Demo / Trial

| Document | Reader | Purpose |
|----------|--------|---------|
| [Trial Entry Guide](trial-entry-guide.md) | Trial users | How to access and explore the platform |
| [Demo Data Guide](demo-data-guide.md) | Demo operators | Seed data structure and usage |
| [Demo Walkthrough](demo-walkthrough.md) | Trial users | Guided tour of features |
| [Demo Acceptance Checklist](demo-acceptance-checklist.md) | QA | Pre-demo verification |
| [User Feedback Template](user-feedback-template.md) | Trial users | Structured feedback form |
| [Product Feedback Taxonomy](product-feedback-taxonomy.md) | PM | Feedback classification |
| [User Trial Triage Guide](user-trial-triage-guide.md) | PM | 8-step feedback triage process |
| [Alpha/Beta Trial Plan](alpha-beta-trial-plan.md) | PM | Trial phases, success criteria |

## Deployment

| Document | Reader | Purpose |
|----------|--------|---------|
| [Deployment Guide](deployment-guide.md) | DevOps | Local/Docker/Production deployment |
| [Production Deployment Runbook](production-deployment-runbook.md) | DevOps | Production deploy steps, verification |
| [Model Provider Production Setup](model-provider-production-setup.md) | DevOps/Admin | Real model API key configuration |
| [GitHub OAuth Production Setup](github-oauth-production-setup.md) | DevOps/Admin | GitHub OAuth App setup |

## Operations

| Document | Reader | Purpose |
|----------|--------|---------|
| [Production Observability Runbook](production-observability-runbook.md) | On-call | Daily monitoring, log patterns |
| [Production Alerting Rules](production-alerting-rules.md) | On-call | Alert severity levels, thresholds |
| [Production Security Hardening Checklist](production-security-hardening-checklist.md) | DevOps | Secrets, CORS, JWT, headers |
| [Incident Response Runbook](incident-response-runbook.md) | On-call | Incident triage, recovery, rollback |

## Security

| Document | Reader | Purpose |
|----------|--------|---------|
| [Production Security Hardening Checklist](production-security-hardening-checklist.md) | DevOps | Security configuration checklist |
| [Incident Response Runbook](incident-response-runbook.md) | On-call | Security incident response |
| [Environment Variable Index](environment-variable-index.md) | DevOps | Security levels for all env vars |

## Model Provider

| Document | Reader | Purpose |
|----------|--------|---------|
| [Model Provider Production Setup](model-provider-production-setup.md) | Admin | Configure OpenAI/Claude/DeepSeek/Qwen/Gemini |

## GitHub Integration

| Document | Reader | Purpose |
|----------|--------|---------|
| [GitHub OAuth Production Setup](github-oauth-production-setup.md) | Admin | Register GitHub OAuth App |

## Product / Roadmap

| Document | Reader | Purpose |
|----------|--------|---------|
| [Roadmap](roadmap.md) | Everyone | v1.0 → v2.0 plan |
| [CHANGELOG.md](../CHANGELOG.md) | Everyone | Release history |
| [Release Notes Template](release-notes-template.md) | PM | Standard release notes format |
| [Release QA Report](release-qa-report.md) | QA | QA summary for releases |

## Milestones (Execution History)

Milestones 6-29 document the implementation journey:

| Milestone | Document | Area |
|-----------|----------|------|
| M6 | [milestone-6-agent-orchestrator-model-gateway.md](milestone-6-agent-orchestrator-model-gateway.md) | Agent Orchestrator + Model Gateway |
| M7 | [milestone-7-rag-knowledge-base.md](milestone-7-rag-knowledge-base.md) | RAG Knowledge Base |
| M8 | [milestone-8-rag-chat-agent-integration.md](milestone-8-rag-chat-agent-integration.md) | RAG + Chat + Agent Integration |
| M9 | [milestone-9-real-model-gateway.md](milestone-9-real-model-gateway.md) | Real Model Gateway |
| M10 | [milestone-10-real-streaming-chat.md](milestone-10-real-streaming-chat.md) | Real SSE Streaming Chat |
| M11 | [milestone-11-devops-observability.md](milestone-11-devops-observability.md) | DevOps + Observability |
| M12 | [milestone-12-frontend-console.md](milestone-12-frontend-console.md) | Frontend Console |
| M13 | [milestone-13-frontend-polish-and-qa.md](milestone-13-frontend-polish-and-qa.md) | Frontend Polish & QA |
| M14 | [milestone-14-frontend-backend-integration-release.md](milestone-14-frontend-backend-integration-release.md) | Integration Release |
| M15 | [milestone-15-automated-testing-demo-data.md](milestone-15-automated-testing-demo-data.md) | Automated Testing |
| M16 | [milestone-16-real-model-gateway-production-hardening.md](milestone-16-real-model-gateway-production-hardening.md) | Production Hardening |
| M16a | [milestone-16a-frontend-dynamic-tech-ui-redesign.md](milestone-16a-frontend-dynamic-tech-ui-redesign.md) | UI Redesign |
| M17 | [milestone-17-github-oauth-pr-review.md](milestone-17-github-oauth-pr-review.md) | GitHub OAuth + PR Review |
| M18 | [milestone-18-cicd-docker-deployment.md](milestone-18-cicd-docker-deployment.md) | CI/CD + Docker |
| M19 | [milestone-19-release-demo-qa-acceptance.md](milestone-19-release-demo-qa-acceptance.md) | Release QA |
| M20a | [milestone-20a-ui-visual-upgrade-phase-2.md](milestone-20a-ui-visual-upgrade-phase-2.md) | UI Upgrade |
| M20b | [milestone-20b-ui-polish-and-consistency.md](milestone-20b-ui-polish-and-consistency.md) | UI Polish |
| M21 | [milestone-21-cloud-demo-production-deployment.md](milestone-21-cloud-demo-production-deployment.md) | Cloud Deploy |
| M22 | [milestone-22-real-model-github-oauth-production-validation.md](milestone-22-real-model-github-oauth-production-validation.md) | Provider Validation |
| M23 | [milestone-23-observability-alerting-security-hardening.md](milestone-23-observability-alerting-security-hardening.md) | Alerting + Security |
| M24 | [milestone-24-productized-demo-user-trial.md](milestone-24-productized-demo-user-trial.md) | Demo + Trial |
| M25 | [milestone-25-feedback-loop-product-iteration.md](milestone-25-feedback-loop-product-iteration.md) | Feedback Loop |
| M26 | [milestone-26-public-website-trial-entry.md](milestone-26-public-website-trial-entry.md) | Public Website |
| M27 | [milestone-27-e2e-stability-release-quality.md](milestone-27-e2e-stability-release-quality.md) | E2E Stability |
| M28 | [milestone-28-frontend-performance-bundle-governance.md](milestone-28-frontend-performance-bundle-governance.md) | Bundle Optimization |
| M29 | [milestone-29-backend-test-coverage-quality-gates.md](milestone-29-backend-test-coverage-quality-gates.md) | Test Coverage |
| M30 | [milestone-30-final-delivery-handoff.md](milestone-30-final-delivery-handoff.md) | Final Delivery |

## Quick Reference Cards

| Need | Go To |
|------|-------|
| API endpoints | [API / Page / Script Index](api-page-script-index.md) |
| All env vars | [Environment Variable Index](environment-variable-index.md) |
| All scripts | [API / Page / Script Index § Scripts](api-page-script-index.md#3-script-index) |
| Release checklist | [Final Release Checklist](final-release-checklist.md) |
| New maintainer | [Project Handoff Guide](project-handoff-guide.md) |
