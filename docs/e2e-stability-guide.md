# E2E Stability Guide

How to write, debug, and maintain reliable Playwright E2E tests for the AI Coding Platform frontend.

## 1. Selector Priority

Always use selectors in this order:

| Priority | API | Example | When to Use |
|----------|-----|---------|-------------|
| 1 | `page.getByTestId()` | `page.getByTestId('btn-create-project')` | Always preferred — requires `data-testid` on element |
| 2 | `page.getByRole()` | `page.getByRole('button', { name: 'Create' })` | For standard HTML elements with ARIA roles |
| 3 | `page.getByLabel()` | `page.getByLabel('Email')` | For form inputs with associated labels |
| 4 | `page.getByText()` | `page.getByText('Model Gateway')` | For unique visible text |
| 5 | `page.getByPlaceholder()` | `page.getByPlaceholder('New session...')` | For inputs with stable placeholders |
| 6 | `page.locator('css-selector')` | `page.locator('.tcb-user')` | Last resort — only for structural elements |

**Never use:**
- `.nth()` on dynamic lists without a stable filter first
- Element Plus internal CSS classes (`.el-input__inner`, `.el-button--primary`, etc.)
- Selectors that depend on animation state or CSS transitions
- Selectors that depend on specific text content that may change with i18n

## 2. Test Data Isolation

### Unique Naming

```ts
const SUFFIX = Date.now().toString()
const PROJECT_NAME = `E2E-Proj-${SUFFIX}`
```

- Every test creates data with a unique suffix
- No test depends on demo data existing (e.g., "Demo AI Workspace")
- Tests can run in any order

### No Demo Data Dependency

E2E tests use `admin@example.com` for login but do NOT rely on:
- Demo AI Workspace existing
- Product Knowledge Base existing
- Chat sessions being pre-created

Demo-specific testing belongs in `scripts/demo-smoke-test.sh`, not in E2E tests.

### Cleanup

If a DELETE API is available, clean up created test data in `afterEach`. If not, unique naming prevents conflicts.

## 3. Wait Strategies

### API-Based Waits (Preferred)

```ts
// Wait for POST response
await Promise.all([
  page.waitForResponse(
    resp => resp.url().includes('/api/projects') && resp.request().method() === 'POST' && resp.status() === 200,
    { timeout: 15000 },
  ),
  page.getByTestId('btn-submit-project').click(),
])
```

### URL-Based Waits

```ts
await expect(page).toHaveURL(/\/projects\/\d+/, { timeout: 8000 })
```

### Element Visibility

```ts
const dialog = page.getByTestId('dialog-create-project')
await expect(dialog).toBeVisible({ timeout: 5000 })
// ... interact ...
await expect(dialog).not.toBeVisible({ timeout: 8000 })
```

### SSE Streaming

For chat SSE tests, wait for the streaming indicator to appear AND disappear:

```ts
// Wait for streaming to complete
await expect(page.locator('.chat-msg--streaming')).not.toBeVisible({ timeout: 30000 })
```

### Never Use fixed `waitForTimeout`

```ts
// BAD — fragile, environment-dependent
await page.waitForTimeout(2000)

// GOOD — wait for a real condition
await expect(page.locator('.el-alert--error')).toBeVisible({ timeout: 10000 })
```

The only exception is when waiting for an SSE stream that has no reliable completion signal. In this case, document the reason.

## 4. Dialog Testing Pattern

```ts
// 1. Click trigger button
await page.getByTestId('btn-create-project').click()

// 2. Wait for dialog to be visible
const dialog = page.getByTestId('dialog-create-project')
await expect(dialog).toBeVisible({ timeout: 5000 })

// 3. Fill form fields
await page.getByTestId('input-project-name').fill('My Project')

// 4. Submit with API response wait
await Promise.all([
  page.waitForResponse(/* API pattern */, { timeout: 15000 }),
  page.getByTestId('btn-submit-project').click(),
])

// 5. Verify dialog closed
await expect(dialog).not.toBeVisible({ timeout: 8000 })

// 6. Verify result in UI
await expect(page.getByTestId('project-table')).toContainText('My Project', { timeout: 8000 })
```

## 5. Table Row Interaction Pattern

```ts
// Find row containing expected text, then interact
const table = page.getByTestId('task-table')
const targetRow = table.locator('tr').filter({ hasText: TASK_TITLE })
await targetRow.getByTestId('btn-execute-task').click()
```

## 6. data-testid Naming Convention

| Element Type | Pattern | Example |
|-------------|---------|---------|
| Buttons (action) | `btn-<action>-<entity>` | `btn-create-project`, `btn-submit-task`, `btn-execute-task` |
| Inputs | `input-<entity>-<field>` | `input-project-name`, `input-task-title` |
| Dialogs | `dialog-<action>-<entity>` | `dialog-create-project`, `dialog-execute-task` |
| Tables | `<entity>-table` | `project-table`, `task-table` |
| Lists/Containers | `<entity>-<container-type>` | `chat-session-list` |
| Select/Dropdown | `select-<entity>-<field>` | `select-task-type` |
| Toggles/Switches | `switch-<entity>-<field>` | `switch-execute-rag` |

Rules:
- All lowercase, hyphenated
- Prefix with element type (`btn-`, `input-`, `dialog-`)
- Use the same entity name across related elements
- Never use `data-testid` for styling
- Never read `data-testid` in business logic
- `data-testid` values are for tests only

## 7. Running E2E Tests

### Local (with dev server already running)

```bash
cd frontend
npx playwright test --workers=1
```

### Local (auto-start dev server)

Playwright config has `webServer` configured. Ensure backend is running, then:

```bash
cd frontend
npm run test:e2e
```

### CI

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

### Debug a specific test

```bash
npx playwright test --workers=1 --debug project-task-chat.spec.ts
```

### Run with headed browser

```bash
npx playwright test --workers=1 --headed
```

## 8. Common Failures & Fixes

### Dialog doesn't open

| Cause | Fix |
|-------|-----|
| Button click didn't trigger | Check `data-testid` is on the correct element (verify in browser DevTools) |
| API error prevents rendering | Check backend is running and healthy |
| Auth token expired | Check login step — increase `login` timeout if backend is slow |

### Element not found after dialog opens

| Cause | Fix |
|-------|-----|
| Element is in Element Plus Teleport (body level) | `getByTestId` works regardless of DOM position |
| Dialog opening animation still running | Increase `toBeVisible` timeout to 5000ms |
| Element is in a different dialog | Use dialog-scoped locators: `dialog.getByTestId(...)` |

### API response wait times out

| Cause | Fix |
|-------|-----|
| Backend is slow | Increase `waitForResponse` timeout to 30000ms |
| Wrong API URL pattern | Check the actual API URL in browser DevTools Network tab |
| POST returns error status | Status check `resp.status() === 200` — if backend returns 4xx/5xx, the wait will fail |

### SSE streaming test is flaky

| Cause | Fix |
|-------|-----|
| Streaming indicator never appears | Ensure Mock provider is working |
| Streaming takes longer than timeout | Increase timeout to 30000ms for streaming completions |
| Chat session selector changed | Verify `data-testid` is on correct element |

## 9. Adding data-testid to a Component

### Vue Component (script setup)

```vue
<template>
  <button data-testid="btn-my-action" @click="handleClick">
    Click Me
  </button>
</template>
```

For Element Plus components, `data-testid` falls through to the root element:

```vue
<el-button type="primary" data-testid="btn-submit-project">Create</el-button>
```

For custom components, ensure the component passes through non-prop attributes (Vue 3 does this automatically for single-root components):

```vue
<!-- GlowButton.vue — single root, data-testid automatically inherits -->
<template>
  <button class="gbtn" @click="$emit('click')">
    <slot />
  </button>
</template>
```

## 10. Release Gate

E2E tests are a **blocking** release gate. Failed E2E tests in `project-task-chat.spec.ts` or `auth.spec.ts` block release.

Steps in CI/Release:
1. `npm run typecheck` — must pass
2. `npm run build` — must pass
3. `npm run test:e2e -- --workers=1` — must pass (all specs)
4. `bash scripts/release-checklist.sh` — must pass

If a test is genuinely broken by a known issue (not flaky), document it in the release notes under "Known Limitations" and create a tracking issue.
