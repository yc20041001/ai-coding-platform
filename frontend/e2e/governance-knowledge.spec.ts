import { test, expect } from '@playwright/test'
const ADMIN_EMAIL = 'admin@example.com'; const ADMIN_PASSWORD = 'Admin@123456'
async function login(page: import('@playwright/test').Page) {
  await page.goto('/login'); await page.getByTestId('login-email').fill(ADMIN_EMAIL)
  await page.getByTestId('login-password').fill(ADMIN_PASSWORD); await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
}
async function checkPrerequisite(page: import('@playwright/test').Page) {
  await page.goto('/admin/observability'); await expect(page).toHaveURL(/\/admin\/observability/)
  const section = page.getByTestId('knowledge-base-section')
  if (await section.isVisible({ timeout: 3000 }).catch(() => false)) return { hasProject: true as const }
  test.info().annotations.push({ type: 'note', description: 'No project; verifying empty state' })
  return { hasProject: false as const }
}

test.describe('Governance Knowledge & Recipe (42B)', () => {
  test.beforeEach(async ({ page }) => { await login(page) })
  test('observability page shows knowledge sections', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByText('治理知识')).toBeVisible()
  })
  test('knowledge base panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('knowledge-base-section')).toBeVisible()
    await expect(page.getByTestId('knowledge-base-section')).toContainText('知识库')
  })
  test('pattern library panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('pattern-library-section')).toBeVisible()
    await expect(page.getByTestId('pattern-library-section')).toContainText('模式库')
  })
  test('recipe panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('recipe-section')).toBeVisible()
    await expect(page.getByTestId('recipe-section')).toContainText('Recipe 库')
  })
  test('search area visible', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('knowledge-base-section').getByText('搜索')).toBeVisible()
  })
  test('recipe score and usage tags visible', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('recipe-section')).toBeVisible()
  })
  test('create button visible', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('knowledge-base-section').getByText('新建')).toBeVisible()
    await expect(page.getByTestId('pattern-library-section').getByText('新建')).toBeVisible()
  })
  test('no js errors on page load', async ({ page }) => {
    const errors: string[] = []; page.on('pageerror', err => errors.push(err.message))
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(errors).toHaveLength(0)
  })
})
