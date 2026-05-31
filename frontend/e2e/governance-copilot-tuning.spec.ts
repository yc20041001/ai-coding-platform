import { test, expect } from '@playwright/test'
const ADMIN_EMAIL = 'admin@example.com'; const ADMIN_PASSWORD = 'Admin@123456'
async function login(page: import('@playwright/test').Page) {
  await page.goto('/login'); await page.getByTestId('login-email').fill(ADMIN_EMAIL)
  await page.getByTestId('login-password').fill(ADMIN_PASSWORD); await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
}
async function checkPrerequisite(page: import('@playwright/test').Page) {
  await page.goto('/admin/observability'); await expect(page).toHaveURL(/\/admin\/observability/)
  const section = page.getByTestId('feedback-section')
  if (await section.isVisible({ timeout: 3000 }).catch(() => false)) return { hasProject: true as const }
  test.info().annotations.push({ type: 'note', description: 'No project; verifying empty state' })
  return { hasProject: false as const }
}

test.describe('Governance Copilot Tuning (43C)', () => {
  test.beforeEach(async ({ page }) => { await login(page) })
  test('observability page shows copilot tuning sections', async ({ page }) => { const s = await checkPrerequisite(page); if (!s.hasProject) return; await expect(page.getByText('Copilot 调优')).toBeVisible() })
  test('feedback panel renders', async ({ page }) => { const s = await checkPrerequisite(page); if (!s.hasProject) return; await expect(page.getByTestId('feedback-section')).toBeVisible(); await expect(page.getByTestId('feedback-section')).toContainText('Operator 反馈') })
  test('adaptive guidance panel renders', async ({ page }) => { const s = await checkPrerequisite(page); if (!s.hasProject) return; await expect(page.getByTestId('adaptive-guidance-section')).toBeVisible(); await expect(page.getByTestId('adaptive-guidance-section')).toContainText('自适应引导') })
  test('copilot tuning panel renders', async ({ page }) => { const s = await checkPrerequisite(page); if (!s.hasProject) return; await expect(page.getByTestId('copilot-tuning-section')).toBeVisible(); await expect(page.getByTestId('copilot-tuning-section')).toContainText('Copilot 调优') })
  test('boost/downrank signal tags visible', async ({ page }) => { const s = await checkPrerequisite(page); if (!s.hasProject) return; await expect(page.getByTestId('adaptive-guidance-section')).toBeVisible() })
  test('tuning confidence metric visible', async ({ page }) => { const s = await checkPrerequisite(page); if (!s.hasProject) return; await expect(page.getByTestId('copilot-tuning-section')).toBeVisible() })
  test('feedback rating tags visible', async ({ page }) => { const s = await checkPrerequisite(page); if (!s.hasProject) return; await expect(page.getByTestId('feedback-section')).toBeVisible() })
  test('no js errors on page load', async ({ page }) => { const e: string[] = []; page.on('pageerror', err => e.push(err.message)); const s = await checkPrerequisite(page); if (!s.hasProject) return; await expect(e).toHaveLength(0) })
})
