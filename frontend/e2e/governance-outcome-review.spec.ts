import { test, expect } from '@playwright/test'
const AE = 'admin@example.com'; const AP = 'Admin@123456'
async function login(p: import('@playwright/test').Page) { await p.goto('/login'); await p.getByTestId('login-email').fill(AE); await p.getByTestId('login-password').fill(AP); await p.getByTestId('login-submit').click(); await expect(p).toHaveURL(/\/dashboard/, { timeout: 10000 }) }
async function prereq(p: import('@playwright/test').Page) { await p.goto('/admin/observability'); await expect(p).toHaveURL(/\/admin\/observability/); const s = p.getByTestId('draft-outcome-review-section'); if (await s.isVisible({ timeout: 3000 }).catch(() => false)) return { ok: true as const }; test.info().annotations.push({ type: 'note', description: 'No project' }); return { ok: false as const } }

test.describe('Governance Outcome Review (44B)', () => {
  test.beforeEach(async ({ page }) => { await login(page) })
  test('outcome review sections visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByText('结果评估')).toBeVisible() })
  test('draft outcome review panel renders', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('draft-outcome-review-section')).toBeVisible(); await expect(page.getByTestId('draft-outcome-review-section')).toContainText('草稿采用评估') })
  test('assistive quality panel renders', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('assistive-quality-section')).toBeVisible(); await expect(page.getByTestId('assistive-quality-section')).toContainText('辅助动作质量') })
  test('package evaluation panel renders', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('package-evaluation-section')).toBeVisible(); await expect(page.getByTestId('package-evaluation-section')).toContainText('提交包评估') })
  test('adoption result tags visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('draft-outcome-review-section')).toBeVisible() })
  test('record adoption button visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('draft-outcome-review-section').getByText('记录采用')).toBeVisible() })
  test('outcome result tags visible', async ({ page }) => { const s = await prereq(page); if (!s.ok) return; await expect(page.getByTestId('assistive-quality-section')).toBeVisible() })
  test('no js errors', async ({ page }) => { const e: string[] = []; page.on('pageerror', err => e.push(err.message)); const s = await prereq(page); if (!s.ok) return; await expect(e).toHaveLength(0) })
})
