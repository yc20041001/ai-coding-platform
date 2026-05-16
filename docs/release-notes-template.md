# Release Notes Template

Copy this template and fill in for each release.

---

# Release vX.Y.Z

**Date**: YYYY-MM-DD
**Audience**: Internal Alpha / External Beta / General Availability

## Highlights

<!-- 2-3 sentences on the most important changes. -->

## Breaking Changes

<!-- List any breaking changes with migration guidance. If none, say "None." -->

## Added

<!-- New features. -->

## Changed

<!-- Changes to existing functionality. -->

## Fixed

<!-- Bug fixes. -->

## Security

<!-- Security improvements or fixes. -->

## Documentation

<!-- New or updated docs. -->

## Upgrade Steps

```
1. Pull latest code: git pull origin main
2. Update .env.production if new variables added (check .env.production.example)
3. Rebuild and restart: bash scripts/prod-deploy.sh up --build
4. Run smoke test: bash scripts/prod-smoke-test.sh <base-url>
5. (If DB migration) Verify Flyway completed successfully in backend logs
```

## Verification Results

| Check | Result |
|-------|--------|
| `mvn test` | ___ passed, ___ failed |
| `npm run typecheck` | ___ |
| `npm run build` | ___ |
| `npm run test:e2e --workers=1` | ___ passed, ___ failed |
| `bash scripts/demo-smoke-test.sh` | ___ |
| `bash scripts/prod-security-check.sh` | ___ |
| `rg` secret scan | ___ |

## Rollback

```
1. Stop services: bash scripts/prod-deploy.sh down
2. Revert code: git checkout <previous-release-tag>
3. Rebuild and start: bash scripts/prod-deploy.sh up --build
4. (If DB migration) Restore from backup: bash scripts/prod-restore-mysql.sh backups/<backup-file>.sql
5. Verify: bash scripts/prod-health-check.sh <base-url>
```

## Known Limitations

<!-- Issues that are known and will be addressed in a future release. -->

## Feedback

- [File a Bug Report](https://github.com/yc20041001/ai-coding-platform/issues/new?template=bug_report.yml)
- [Request a Feature](https://github.com/yc20041001/ai-coding-platform/issues/new?template=feature_request.yml)
- [Submit Trial Feedback](https://github.com/yc20041001/ai-coding-platform/issues/new?template=user_trial_feedback.yml)
