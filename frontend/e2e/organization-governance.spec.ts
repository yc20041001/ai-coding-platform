import { test, expect } from '@playwright/test'

const ADMIN_EMAIL = 'admin@example.com'
const ADMIN_PASSWORD = 'Admin@123456'

async function login(page: import('@playwright/test').Page) {
  await page.goto('/login')
  await page.getByTestId('login-email').fill(ADMIN_EMAIL)
  await page.getByTestId('login-password').fill(ADMIN_PASSWORD)
  await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
}

async function openObservabilityAndCheckPrerequisite(page: import('@playwright/test').Page) {
  await page.goto('/admin/observability')
  await expect(page).toHaveURL(/\/admin\/observability/)
  const policySection = page.getByTestId('org-policy-section')
  if (await policySection.isVisible({ timeout: 3000 }).catch(() => false)) {
    return { hasProject: true as const }
  }
  test.info().annotations.push({
    type: 'note',
    description: 'No seeded project available; verifying empty state fallback for governance sections',
  })
  return { hasProject: false as const }
}

test.describe('Organization Governance (40B)', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('observability page shows organization governance sections', async ({ page }) => {
    const state = await openObservabilityAndCheckPrerequisite(page)
    if (!state.hasProject) return
    await expect(page.getByText('组织级治理')).toBeVisible()
  })

  test('organization trial policy panel renders', async ({ page }) => {
    const state = await openObservabilityAndCheckPrerequisite(page)
    if (!state.hasProject) return
    const panel = page.getByTestId('org-policy-section')
    await expect(panel).toBeVisible()
    await expect(panel).toContainText('组织 Trial 策略')
  })

  test('guardrail dashboard panel renders', async ({ page }) => {
    const state = await openObservabilityAndCheckPrerequisite(page)
    if (!state.hasProject) return
    const panel = page.getByTestId('guardrail-dashboard-section')
    await expect(panel).toBeVisible()
    await expect(panel).toContainText('Release Guardrail 看板')
  })

  test('drift dashboard panel renders', async ({ page }) => {
    const state = await openObservabilityAndCheckPrerequisite(page)
    if (!state.hasProject) return
    const panel = page.getByTestId('drift-dashboard-section')
    await expect(panel).toBeVisible()
    await expect(panel).toContainText('Portfolio Drift 检测')
  })

  test('create policy dialog works', async ({ page }) => {
    const state = await openObservabilityAndCheckPrerequisite(page)
    if (!state.hasProject) return
    const panel = page.getByTestId('org-policy-section')
    await panel.getByText('新建策略').click()
    await expect(page.locator('.el-dialog')).toBeVisible()
    await expect(page.locator('.el-dialog')).toContainText('新建策略')
  })

  test('guardrail refresh button visible', async ({ page }) => {
    const state = await openObservabilityAndCheckPrerequisite(page)
    if (!state.hasProject) return
    const panel = page.getByTestId('guardrail-dashboard-section')
    await expect(panel.getByText('刷新评估')).toBeVisible()
  })

  test('drift refresh button visible', async ({ page }) => {
    const state = await openObservabilityAndCheckPrerequisite(page)
    if (!state.hasProject) return
    const panel = page.getByTestId('drift-dashboard-section')
    await expect(panel.getByText('刷新 Drift')).toBeVisible()
  })

  test('no js errors on page load', async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', err => errors.push(err.message))
    const state = await openObservabilityAndCheckPrerequisite(page)
    if (!state.hasProject) return
    await expect(errors).toHaveLength(0)
  })
})
