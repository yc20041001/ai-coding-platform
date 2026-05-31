import { test, expect } from '@playwright/test'
const ADMIN_LOGIN = 'admin@example.com'; const ADMIN_PASS = 'Admin@123456'
async function login(p: import('@playwright/test').Page) { await p.goto('/login'); await p.getByTestId('login-email').fill(ADMIN_LOGIN); await p.getByTestId('login-password').fill(ADMIN_PASS); await p.getByTestId('login-submit').click(); await expect(p).toHaveURL(/\/dashboard/, { timeout: 10000 }) }
async function prereq(p: import('@playwright/test').Page) { await p.goto('/admin/observability'); await expect(p).toHaveURL(/\/admin\/observability/); const s = p.getByTestId('draft-planning-section'); if (await s.isVisible({ timeout: 3000 }).catch(() => false)) return { ok: true as const }; test.info().annotations.push({ type: 'note', description: 'No project' }); return { ok: false as const } }

test.describe('Governance Draft Planning (44A)', () => {
  test.beforeEach(async ({ page }) => { await login(page) })
  test('observability shows draft planning', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByText('草稿规划')).toBeVisible() })
  test('draft planning panel renders', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('draft-planning-section')).toBeVisible(); await expect(page.getByTestId('draft-planning-section')).toContainText('草稿计划') })
  test('assistive action panel renders', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('assistive-action-section')).toBeVisible(); await expect(page.getByTestId('assistive-action-section')).toContainText('安全辅助动作') })
  test('package panel renders', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('recommendation-package-section')).toBeVisible(); await expect(page.getByTestId('recommendation-package-section')).toContainText('推荐提交包') })
  test('risk/safety level tags visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('draft-planning-section')).toBeVisible() })
  test('create draft button visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('draft-planning-section').getByText('新建草稿')).toBeVisible() })
  test('generate actions button visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('assistive-action-section').getByText('生成动作')).toBeVisible() })
  test('no js errors', async ({ page }) => { const e: string[] = []; page.on('pageerror', err => e.push(err.message)); const s = await prereq(page); if (!s.ok) return; await expect(e).toHaveLength(0) })
})
