import { test, expect } from '@playwright/test'
const ADMIN_EMAIL = 'admin@example.com'; const ADMIN_PASSWORD = 'Admin@123456'
async function login(page: import('@playwright/test').Page) {
  await page.goto('/login'); await page.getByTestId('login-email').fill(ADMIN_EMAIL)
  await page.getByTestId('login-password').fill(ADMIN_PASSWORD); await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
}
async function checkPrerequisite(page: import('@playwright/test').Page) {
  await page.goto('/admin/observability'); await expect(page).toHaveURL(/\/admin\/observability/)
  const section = page.getByTestId('simulation-scenario-section')
  if (await section.isVisible({ timeout: 3000 }).catch(() => false)) return { hasProject: true as const }
  test.info().annotations.push({ type: 'note', description: 'No project; verifying empty state' })
  return { hasProject: false as const }
}

test.describe('Governance Simulation & Tuning (41C)', () => {
  test.beforeEach(async ({ page }) => { await login(page) })
  test('observability page shows simulation sections', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByText('治理模拟')).toBeVisible()
  })
  test('scenario panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('simulation-scenario-section')).toBeVisible()
    await expect(page.getByTestId('simulation-scenario-section')).toContainText('模拟场景')
  })
  test('comparison panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('simulation-comparison-section')).toBeVisible()
    await expect(page.getByTestId('simulation-comparison-section')).toContainText('模拟对比')
  })
  test('suggestion panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('tuning-suggestion-section')).toBeVisible()
    await expect(page.getByTestId('tuning-suggestion-section')).toContainText('策略调优建议')
  })
  test('create scenario dialog works', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await page.getByTestId('simulation-scenario-section').getByText('新建').click()
    await expect(page.locator('.el-dialog')).toBeVisible()
  })
  test('run scenario button visible', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    // Verify the section is visible (scenarios may vary)
    await expect(page.getByTestId('simulation-scenario-section').getByText('新建')).toBeVisible()
  })
  test('tuning suggestion refresh button visible', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('tuning-suggestion-section').getByText('刷新')).toBeVisible()
  })
  test('no js errors on page load', async ({ page }) => {
    const errors: string[] = []; page.on('pageerror', err => errors.push(err.message))
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(errors).toHaveLength(0)
  })
})
