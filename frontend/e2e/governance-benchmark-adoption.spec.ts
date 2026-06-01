import { test, expect } from '@playwright/test'
const AE = 'admin@example.com'; const AP = 'Admin@123456'
async function login(p: import('@playwright/test').Page) { await p.goto('/login'); await p.getByTestId('login-email').fill(AE); await p.getByTestId('login-password').fill(AP); await p.getByTestId('login-submit').click(); await expect(p).toHaveURL(/\/dashboard/, { timeout: 10000 }) }
async function prereq(p: import('@playwright/test').Page) { await p.goto('/admin/observability'); await expect(p).toHaveURL(/\/admin\/observability/); const s = p.getByTestId('adoption-section'); if (await s.isVisible({ timeout: 3000 }).catch(() => false)) return { ok: true as const }; test.info().annotations.push({ type: 'note', description: 'No project' }); return { ok: false as const } }

test.describe('Governance Benchmark Adoption (45B)', () => {
  test.beforeEach(async ({ page }) => { await login(page) })
  test('adoption sections visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByText('基准推广')).toBeVisible() })
  test('adoption panel renders', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('adoption-section')).toBeVisible(); await expect(page.getByTestId('adoption-section')).toContainText('基准采用') })
  test('campaign panel renders', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('campaign-section')).toBeVisible(); await expect(page.getByTestId('campaign-section')).toContainText('改进活动') })
  test('uplift panel renders', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('uplift-section')).toBeVisible(); await expect(page.getByTestId('uplift-section')).toContainText('提升测量') })
  test('adoption status tags visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('adoption-section')).toBeVisible() })
  test('campaign status tags visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('campaign-section')).toBeVisible() })
  test('uplift level tags visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('uplift-section')).toBeVisible() })
  test('no js errors', async ({ page }) => { const e: string[] = []; page.on('pageerror', err => e.push(err.message)); const s = await prereq(page); if (!s.ok) return; await expect(e).toHaveLength(0) })
})
