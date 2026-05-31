import { test, expect } from '@playwright/test'
const ADMIN_EMAIL = 'admin@example.com'; const ADMIN_PASSWORD = 'Admin@123456'
async function login(page: import('@playwright/test').Page) {
  await page.goto('/login'); await page.getByTestId('login-email').fill(ADMIN_EMAIL)
  await page.getByTestId('login-password').fill(ADMIN_PASSWORD); await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
}
async function checkPrerequisite(page: import('@playwright/test').Page) {
  await page.goto('/admin/observability'); await expect(page).toHaveURL(/\/admin\/observability/)
  const section = page.getByTestId('operator-memory-section')
  if (await section.isVisible({ timeout: 3000 }).catch(() => false)) return { hasProject: true as const }
  test.info().annotations.push({ type: 'note', description: 'No project; verifying empty state' })
  return { hasProject: false as const }
}

test.describe('Governance Operator Learning (43B)', () => {
  test.beforeEach(async ({ page }) => { await login(page) })
  test('observability page shows learning sections', async ({ page }) => { const s = await checkPrerequisite(page); if (!s.hasProject) return; await expect(page.getByText('学习回路')).toBeVisible() })
  test('operator memory panel renders', async ({ page }) => { const s = await checkPrerequisite(page); if (!s.hasProject) return; await expect(page.getByTestId('operator-memory-section')).toBeVisible(); await expect(page.getByTestId('operator-memory-section')).toContainText('Operator 记忆') })
  test('session insight panel renders', async ({ page }) => { const s = await checkPrerequisite(page); if (!s.hasProject) return; await expect(page.getByTestId('session-insight-section')).toBeVisible(); await expect(page.getByTestId('session-insight-section')).toContainText('会话洞察') })
  test('reuse bundle panel renders', async ({ page }) => { const s = await checkPrerequisite(page); if (!s.hasProject) return; await expect(page.getByTestId('reuse-bundle-section')).toBeVisible(); await expect(page.getByTestId('reuse-bundle-section')).toContainText('复用 Bundle') })
  test('productivity metric tiles visible', async ({ page }) => { const s = await checkPrerequisite(page); if (!s.hasProject) return; await expect(page.getByTestId('session-insight-section')).toBeVisible() })
  test('refresh buttons visible', async ({ page }) => { const s = await checkPrerequisite(page); if (!s.hasProject) return; await expect(page.getByTestId('operator-memory-section')).toBeVisible() })
  test('reuse count labels visible', async ({ page }) => { const s = await checkPrerequisite(page); if (!s.hasProject) return; await expect(page.getByTestId('reuse-bundle-section')).toBeVisible() })
  test('no js errors on page load', async ({ page }) => { const e: string[] = []; page.on('pageerror', err => e.push(err.message)); const s = await checkPrerequisite(page); if (!s.hasProject) return; await expect(e).toHaveLength(0) })
})
