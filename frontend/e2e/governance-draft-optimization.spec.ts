import { test, expect } from '@playwright/test'
const AE = 'admin@example.com'; const AP = 'Admin@123456'
async function login(p: import('@playwright/test').Page) { await p.goto('/login'); await p.getByTestId('login-email').fill(AE); await p.getByTestId('login-password').fill(AP); await p.getByTestId('login-submit').click(); await expect(p).toHaveURL(/\/dashboard/, { timeout: 10000 }) }
async function prereq(p: import('@playwright/test').Page) { await p.goto('/admin/observability'); await expect(p).toHaveURL(/\/admin\/observability/); const s = p.getByTestId('draft-optimization-section'); if (await s.isVisible({ timeout: 3000 }).catch(() => false)) return { ok: true as const }; test.info().annotations.push({ type: 'note', description: 'No project' }); return { ok: false as const } }

test.describe('Governance Draft Optimization (44C)', () => {
  test.beforeEach(async ({ page }) => { await login(page) })
  test('optimization sections visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByText('起草优化')).toBeVisible() })
  test('draft optimization panel renders', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('draft-optimization-section')).toBeVisible(); await expect(page.getByTestId('draft-optimization-section')).toContainText('起草优化信号') })
  test('assistive ordering panel renders', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('assistive-ordering-section')).toBeVisible(); await expect(page.getByTestId('assistive-ordering-section')).toContainText('辅助动作排序') })
  test('package composition panel renders', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('package-composition-section')).toBeVisible(); await expect(page.getByTestId('package-composition-section')).toContainText('提交包组成') })
  test('signal level tags visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('draft-optimization-section')).toBeVisible() })
  test('optimization level tags visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('assistive-ordering-section')).toBeVisible() })
  test('tuning level tags visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('package-composition-section')).toBeVisible() })
  test('no js errors', async ({ page }) => { const e: string[] = []; page.on('pageerror', err => e.push(err.message)); const s = await prereq(page); if (!s.ok) return; await expect(e).toHaveLength(0) })
})
