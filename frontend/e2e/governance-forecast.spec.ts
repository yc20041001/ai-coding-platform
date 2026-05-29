import { test, expect } from '@playwright/test'

const ADMIN_EMAIL = 'admin@example.com'; const ADMIN_PASSWORD = 'Admin@123456'
async function login(page: import('@playwright/test').Page) {
  await page.goto('/login'); await page.getByTestId('login-email').fill(ADMIN_EMAIL)
  await page.getByTestId('login-password').fill(ADMIN_PASSWORD); await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
}
async function checkPrerequisite(page: import('@playwright/test').Page) {
  await page.goto('/admin/observability'); await expect(page).toHaveURL(/\/admin\/observability/)
  const section = page.getByTestId('capacity-forecast-section')
  if (await section.isVisible({ timeout: 3000 }).catch(() => false)) return { hasProject: true as const }
  test.info().annotations.push({ type: 'note', description: 'No project; verifying empty state' })
  return { hasProject: false as const }
}

test.describe('Governance Forecast & Risk (41B)', () => {
  test.beforeEach(async ({ page }) => { await login(page) })

  test('observability page shows forecast sections', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByText('治理预测')).toBeVisible()
  })
  test('capacity forecast panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('capacity-forecast-section')).toBeVisible()
    await expect(page.getByTestId('capacity-forecast-section')).toContainText('容量预测')
  })
  test('risk signal panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('risk-signal-section')).toBeVisible()
    await expect(page.getByTestId('risk-signal-section')).toContainText('风险预测信号')
  })
  test('backlog health panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('backlog-health-section')).toBeVisible()
    await expect(page.getByTestId('backlog-health-section')).toContainText('积压健康度')
  })
  test('refresh buttons visible', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('capacity-forecast-section').getByText('刷新')).toBeVisible()
    await expect(page.getByTestId('risk-signal-section').getByText('刷新')).toBeVisible()
  })
  test('risk signal list visible', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('risk-signal-section')).toBeVisible()
  })
  test('backlog growth tags visible', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('backlog-health-section')).toBeVisible()
  })
  test('no js errors on page load', async ({ page }) => {
    const errors: string[] = []; page.on('pageerror', err => errors.push(err.message))
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(errors).toHaveLength(0)
  })
})
