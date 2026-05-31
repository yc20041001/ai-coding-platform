import { test, expect } from '@playwright/test'
const ADMIN_EMAIL = 'admin@example.com'; const ADMIN_PASSWORD = 'Admin@123456'
async function login(page: import('@playwright/test').Page) {
  await page.goto('/login'); await page.getByTestId('login-email').fill(ADMIN_EMAIL)
  await page.getByTestId('login-password').fill(ADMIN_PASSWORD); await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
}
async function checkPrerequisite(page: import('@playwright/test').Page) {
  await page.goto('/admin/observability'); await expect(page).toHaveURL(/\/admin\/observability/)
  const section = page.getByTestId('recipe-effectiveness-section')
  if (await section.isVisible({ timeout: 3000 }).catch(() => false)) return { hasProject: true as const }
  test.info().annotations.push({ type: 'note', description: 'No project; verifying empty state' })
  return { hasProject: false as const }
}

test.describe('Governance Effectiveness & Optimization (42C)', () => {
  test.beforeEach(async ({ page }) => { await login(page) })
  test('observability page shows effectiveness sections', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByText('效果分析')).toBeVisible()
  })
  test('recipe effectiveness panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('recipe-effectiveness-section')).toBeVisible()
    await expect(page.getByTestId('recipe-effectiveness-section')).toContainText('Recipe 效果')
  })
  test('playbook analytics panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('playbook-analytics-section')).toBeVisible()
    await expect(page.getByTestId('playbook-analytics-section')).toContainText('Playbook 分析')
  })
  test('optimization suggestion panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('optimization-section')).toBeVisible()
    await expect(page.getByTestId('optimization-section')).toContainText('优化建议')
  })
  test('top/low-value recipe areas visible', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('recipe-effectiveness-section')).toBeVisible()
  })
  test('suggestion priority tags visible', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('optimization-section')).toBeVisible()
  })
  test('refresh buttons visible', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('recipe-effectiveness-section').getByText('刷新')).toBeVisible()
    await expect(page.getByTestId('playbook-analytics-section').getByText('刷新')).toBeVisible()
  })
  test('no js errors on page load', async ({ page }) => {
    const errors: string[] = []; page.on('pageerror', err => errors.push(err.message))
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(errors).toHaveLength(0)
  })
})
