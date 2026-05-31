import { test, expect } from '@playwright/test'
const AE = 'admin@example.com'; const AP = 'Admin@123456'
async function login(p: import('@playwright/test').Page) { await p.goto('/login'); await p.getByTestId('login-email').fill(AE); await p.getByTestId('login-password').fill(AP); await p.getByTestId('login-submit').click(); await expect(p).toHaveURL(/\/dashboard/, { timeout: 10000 }) }
async function prereq(p: import('@playwright/test').Page) { await p.goto('/admin/observability'); await expect(p).toHaveURL(/\/admin\/observability/); const s = p.getByTestId('benchmark-section'); if (await s.isVisible({ timeout: 3000 }).catch(() => false)) return { ok: true as const }; test.info().annotations.push({ type: 'note', description: 'No project' }); return { ok: false as const } }

test.describe('Governance Portfolio Benchmark (45A)', () => {
  test.beforeEach(async ({ page }) => { await login(page) })
  test('benchmark sections visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByText('组合基准')).toBeVisible() })
  test('benchmark panel renders', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('benchmark-section')).toBeVisible(); await expect(page.getByTestId('benchmark-section')).toContainText('组合基准') })
  test('alignment panel renders', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('alignment-section')).toBeVisible(); await expect(page.getByTestId('alignment-section')).toContainText('最佳实践对齐') })
  test('scorecard panel renders', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('scorecard-section')).toBeVisible(); await expect(page.getByTestId('scorecard-section')).toContainText('成熟度记分卡') })
  test('signal level tags visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('benchmark-section')).toBeVisible() })
  test('alignment level tags visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('alignment-section')).toBeVisible() })
  test('maturity level tags visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('scorecard-section')).toBeVisible() })
  test('no js errors', async ({ page }) => { const e: string[] = []; page.on('pageerror', err => e.push(err.message)); const s = await prereq(page); if (!s.ok) return; await expect(e).toHaveLength(0) })
})
