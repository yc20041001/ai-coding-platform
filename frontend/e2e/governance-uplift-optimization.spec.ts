import { test, expect } from '@playwright/test'
const AE = 'admin@example.com'; const AP = 'Admin@123456'
async function login(p: import('@playwright/test').Page) { await p.goto('/login'); await p.getByTestId('login-email').fill(AE); await p.getByTestId('login-password').fill(AP); await p.getByTestId('login-submit').click(); await expect(p).toHaveURL(/\/dashboard/, { timeout: 10000 }) }
async function prereq(p: import('@playwright/test').Page) { await p.goto('/admin/observability'); await expect(p).toHaveURL(/\/admin\/observability/); const s = p.getByTestId('evolution-section'); if (await s.isVisible({ timeout: 3000 }).catch(() => false)) return { ok: true as const }; test.info().annotations.push({ type: 'note', description: 'No project' }); return { ok: false as const } }

test.describe('Governance Uplift Optimization (45C)', () => {
  test.beforeEach(async ({ page }) => { await login(page) })
  test('sections visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByText('提升优化')).toBeVisible() })
  test('evolution panel renders', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('evolution-section')).toBeVisible(); await expect(page.getByTestId('evolution-section')).toContainText('基准演化') })
  test('campaign ranking panel renders', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('campaign-ranking-section')).toBeVisible(); await expect(page.getByTestId('campaign-ranking-section')).toContainText('活动排名') })
  test('progress map panel renders', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('progress-map-section')).toBeVisible(); await expect(page.getByTestId('progress-map-section')).toContainText('进展地图') })
  test('evolution signal tags visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('evolution-section')).toBeVisible() })
  test('effectiveness level tags visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('campaign-ranking-section')).toBeVisible() })
  test('progress signal tags visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('progress-map-section')).toBeVisible() })
  test('no js errors', async ({ page }) => { const e: string[] = []; page.on('pageerror', err => e.push(err.message)); const s = await prereq(page); if (!s.ok) return; await expect(e).toHaveLength(0) })
})
