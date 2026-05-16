# Backend Test Coverage Report

> Generated: YYYY-MM-DD
> Total tests: N
> Passed: N | Failed: 0 | Skipped: 0

## 1. Test Execution Summary

```
mvn test output:
Tests run: N, Failures: 0, Errors: 0, Skipped: 0
```

## 2. Coverage by Module

| Module | Unit Tests | Integration Tests | Total | Status |
|--------|-----------|-------------------|-------|--------|
| Auth / JWT | | | | |
| Project / Member | | | | |
| Task / Agent | | | | |
| Chat / SSE | | | | |
| RAG / Knowledge | | | | |
| Model Gateway | | | | |
| GitHub / PR Review | | | | |
| Observability / Audit | | | | |

## 3. Critical Path Coverage

| Path | Covered | Tests |
|------|---------|-------|
| Login → Token → Protected API | | |
| Create Task → Execute → Complete | | |
| Send Chat → SSE → Complete | | |
| Upload Doc → Chunk → Search | | |
| Model Gateway → Safety → Mask → Cost | | |
| PR Review → Parse → Validate → Output | | |
| No Token → UNAUTHORIZED | | |
| Refresh Token → Protected API → REJECTED | | |
| Wrong Role → ACCESS DENIED | | |

## 4. Error Path Coverage

| Error Scenario | Covered | Tests |
|---------------|---------|-------|
| Invalid credentials | | |
| Expired/Invalid token | | |
| Missing required fields | | |
| Duplicate resource | | |
| Resource not found | | |
| Illegal state transition | | |
| Rate limit / Timeout | | |
| External service failure | | |

## 5. Security Coverage

| Security Check | Covered | Tests |
|---------------|---------|-------|
| Token type verification (access/refresh) | | |
| Refresh token rejected for API access | | |
| Access token rejected for refresh | | |
| Tampered token rejection | | |
| Prompt safety — blocked patterns | | |
| Prompt safety — warning patterns | | |
| API key masking | | |
| API key in logs sanitization | | |

## 6. Known Gaps

(List any known coverage gaps and their risk assessment)

## 7. Recommendations

(List recommendations for improving coverage in future milestones)
