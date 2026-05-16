# Backend Test Matrix

## Overview

| Module | Risk | Test Type | Current Tests | Required Tests | Priority | Status |
|--------|------|-----------|---------------|----------------|----------|--------|
| Auth / JWT | Token misuse, privilege escalation | Unit + Integration | 7 (login, me, no-token, refresh, access-for-refresh, wrong-pw, missing-email) | access/refresh type assertion, refresh-token→protected, tampered token, expired token | P0 | ✅ |
| Project / Member | Permission bypass | Integration | 5 (create, detail, overview, members, no-token) | owner/viewer/maintainer role check | P1 | ✅ |
| Task / Agent | State corruption, double-execute | Unit + Integration | 4 (create, execute→completed, repeat rejection, no-token) | state machine illegal transitions, retry boundary, cancel | P0 | ✅ |
| Chat SSE | Stream lifecycle, message loss | Integration | 4 (create session, send msg, list sessions, no-token) | message status STREAMING→COMPLETED, references, non-member denied | P1 | ✅ |
| RAG | Bad chunks, empty search | Unit + Integration | 4 (create KB, upload doc+chunks, search, no-token) | chunk split/overlap/hash, empty search, limit | P1 | ✅ |
| Model Gateway | Cost/safety/secret leak | Unit + Integration | 13 (safety 9, masking 7, pricing 5, integration 7) | safety patterns, masking edge cases, pricing overrides, fallback | P0 | ✅ |
| GitHub PR Review | External API, parsing | Unit | 15+ (parse JSON 7, validate risk 4, build prompts 2, properties 4) | bad output fallback, empty/long patch, token-in-prompt check | P1 | ✅ |
| Observability / Audit | Missing audit trail | Integration | 0 dedicated | audit log not blocking flow | P2 | Deferred |

## Test Count Summary

| Test Class | Type | Count | Module |
|------------|------|-------|--------|
| `AuthIntegrationTest` | Integration | 7 | Auth |
| `ProjectIntegrationTest` | Integration | 5 | Project |
| `TaskOrchestratorIntegrationTest` | Integration | 4 | Task |
| `TaskStateMachineTest` | Unit | 8+ | Task |
| `ChatIntegrationTest` | Integration | 4 | Chat |
| `RagIntegrationTest` | Integration | 4 | RAG |
| `DocumentChunkServiceTest` | Unit | 8+ | RAG |
| `PromptSafetyServiceTest` | Unit | 9 | Model Gateway |
| `ModelSecretMaskingServiceTest` | Unit | 7 | Model Gateway |
| `ModelPricingServiceTest` | Unit | 5 | Model Gateway |
| `ModelGatewayIntegrationTest` | Integration | 7 | Model Gateway |
| `PrReviewApplicationServiceTest` | Unit | 15+ | GitHub |
| `GithubPropertiesTest` | Unit | 4 | GitHub |
| `JwtTokenProviderTest` | Unit | 8+ | Security |

## Risk Definitions

- **P0**: Failure would allow data loss, security breach, or system unavailability
- **P1**: Failure would break key user workflows or cause incorrect results
- **P2**: Failure would degrade observability but not block core flows

## Coverage Gaps (Post-M29)

- Observability/Audit module has no dedicated tests (P2 — deferred)
- Model Gateway error code mapping from provider responses (partial — integration tests verify MOCK provider)
- SSE real-time streaming behavior (integration tests verify state transitions, not real-time token emission)

## How to Run

```bash
cd backend
mvn test                              # all tests
mvn test -pl -Dtest=JwtTokenProviderTest  # single test class
```
