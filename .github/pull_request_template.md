## Summary

<!-- One to two sentences describing what this PR does and why. -->

## Type of Change

- [ ] Bug fix
- [ ] New feature
- [ ] Enhancement / improvement
- [ ] Refactor (no functional change)
- [ ] Documentation
- [ ] CI / build / tooling
- [ ] Security fix

## User Impact

<!-- How does this change affect the end user? If no user impact, say "None." -->
<!-- For UI changes, describe what the user will see differently. -->

## Screenshots / Recordings

<!-- If this PR changes the UI, include before/after screenshots or a short recording. -->

## Verification

### Backend

- [ ] `mvn test` — all tests pass
- [ ] `mvn clean compile` — no errors
- [ ] No new compiler warnings
- [ ] API behavior verified manually

### Frontend

- [ ] `npm run typecheck` — clean
- [ ] `npm run build` — succeeds
- [ ] `npm run test:e2e -- --workers=1` — all pass or known flaky documented
- [ ] UI verified in browser

### Security

- [ ] No secrets in code, comments, logs, or test fixtures
- [ ] `rg "sk-[a-zA-Z0-9]{20,}|ghp_[a-zA-Z0-9]{20,}|github_pat_" .` — clean
- [ ] `.env` / `.env.production` not included in changes
- [ ] API responses do not leak tokens or keys

## Migration / Rollback

<!-- If this PR includes DB migrations, config changes, or dependency updates, describe how to upgrade and roll back. -->
<!-- If none, say "None." -->

- **Upgrade steps:**
- **Rollback steps:**

## Known Limitations

<!-- Any edge cases, known issues, or follow-up work. -->

## Linked Issue

<!-- Closes #123 or Related to #456 -->
