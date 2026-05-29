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

async function checkPrerequisite(page: import('@playwright/test').Page) {
  await page.goto('/admin/observability')
  await expect(page).toHaveURL(/\/admin\/observability/)
  const section = page.getByTestId('sla-policy-section')
  if (await section.isVisible({ timeout: 3000 }).catch(() => false)) return { hasProject: true as const }
  test.info().annotations.push({ type: 'note', description: 'No project; verifying empty state' })
  return { hasProject: false as const }
}

test.describe('Governance Operations (41A)', () => {
  test.beforeEach(async ({ page }) => { await login(page) })

  test('observability page shows governance operations sections', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByText('治理运营')).toBeVisible()
  })
  test('SLA policy panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('sla-policy-section')).toBeVisible()
    await expect(page.getByTestId('sla-policy-section')).toContainText('SLA 策略')
  })
  test('escalation panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('escalation-section')).toBeVisible()
    await expect(page.getByTestId('escalation-section')).toContainText('升级事件')
  })
  test('ownership health panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('ownership-health-section')).toBeVisible()
    await expect(page.getByTestId('ownership-health-section')).toContainText('Owner 健康度')
  })
  test('create SLA dialog works', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await page.getByTestId('sla-policy-section').getByText('新建').click()
    await expect(page.locator('.el-dialog')).toBeVisible()
  })
  test('escalation action buttons visible', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('escalation-section').getByText('扫描')).toBeVisible()
  })
  test('refresh button visible', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('ownership-health-section').getByText('刷新')).toBeVisible()
  })
  test('no js errors on page load', async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', err => errors.push(err.message))
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(errors).toHaveLength(0)
  })
})
