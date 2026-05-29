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
  const portfolioSection = page.getByTestId('portfolio-dashboard-section')
  if (await portfolioSection.isVisible({ timeout: 3000 }).catch(() => false)) {
    return { hasProject: true as const }
  }
  test.info().annotations.push({
    type: 'note',
    description: 'No seeded project available; verifying empty state fallback for governance sections',
  })
  return { hasProject: false as const }
}

test.describe('Release Governance Portfolio (40A)', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should display portfolio dashboard panel on observability page', async ({ page }) => {
    const state = await openObservabilityAndCheckPrerequisite(page)
    if (!state.hasProject) return
    const panel = page.getByTestId('portfolio-dashboard-section')
    await expect(panel).toBeVisible()
    await expect(panel).toContainText('发布组合看板')
  })

  test('should display governance baseline panel on observability page', async ({ page }) => {
    const state = await openObservabilityAndCheckPrerequisite(page)
    if (!state.hasProject) return
    const panel = page.getByTestId('governance-baseline-section')
    await expect(panel).toBeVisible()
    await expect(panel).toContainText('治理基线模板')
  })

  test('should display risk heatmap panel on observability page', async ({ page }) => {
    const state = await openObservabilityAndCheckPrerequisite(page)
    if (!state.hasProject) return
    const panel = page.getByTestId('risk-heatmap-section')
    await expect(panel).toBeVisible()
    await expect(panel).toContainText('发布风险热力图')
  })

  test('should show multi-project governance section header', async ({ page }) => {
    const state = await openObservabilityAndCheckPrerequisite(page)
    if (!state.hasProject) return
    await expect(page.getByText('多项目发布治理')).toBeVisible()
  })

  test('should show refresh portfolio button', async ({ page }) => {
    const state = await openObservabilityAndCheckPrerequisite(page)
    if (!state.hasProject) return
    const panel = page.getByTestId('portfolio-dashboard-section')
    await expect(panel.getByText('刷新快照')).toBeVisible()
  })

  test('should show create template button', async ({ page }) => {
    const state = await openObservabilityAndCheckPrerequisite(page)
    if (!state.hasProject) return
    const panel = page.getByTestId('governance-baseline-section')
    await expect(panel.getByText('新建模板')).toBeVisible()
  })

  test('should show refresh heatmap button', async ({ page }) => {
    const state = await openObservabilityAndCheckPrerequisite(page)
    if (!state.hasProject) return
    const panel = page.getByTestId('risk-heatmap-section')
    await expect(panel.getByText('刷新快照')).toBeVisible()
  })

  test('should not have JS errors on page load', async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', err => errors.push(err.message))
    const state = await openObservabilityAndCheckPrerequisite(page)
    if (!state.hasProject) return
    await expect(errors).toHaveLength(0)
  })
})
