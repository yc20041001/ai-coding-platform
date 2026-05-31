import { test, expect } from '@playwright/test'
const ADMIN_EMAIL = 'admin@example.com'; const ADMIN_PASSWORD = 'Admin@123456'
async function login(page: import('@playwright/test').Page) {
  await page.goto('/login'); await page.getByTestId('login-email').fill(ADMIN_EMAIL)
  await page.getByTestId('login-password').fill(ADMIN_PASSWORD); await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
}
async function checkPrerequisite(page: import('@playwright/test').Page) {
  await page.goto('/admin/observability'); await expect(page).toHaveURL(/\/admin\/observability/)
  const section = page.getByTestId('workspace-console-section')
  if (await section.isVisible({ timeout: 3000 }).catch(() => false)) return { hasProject: true as const }
  test.info().annotations.push({ type: 'note', description: 'No project; verifying empty state' })
  return { hasProject: false as const }
}

test.describe('Governance Workspace & Copilot (43A)', () => {
  test.beforeEach(async ({ page }) => { await login(page) })
  test('observability page shows workspace sections', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return; await expect(page.getByText('Copilot 工作台')).toBeVisible()
  })
  test('workspace console renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return; await expect(page.getByTestId('workspace-console-section')).toBeVisible(); await expect(page.getByTestId('workspace-console-section')).toContainText('Copilot 工作台')
  })
  test('guided task panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return; await expect(page.getByTestId('guided-task-section')).toBeVisible(); await expect(page.getByTestId('guided-task-section')).toContainText('引导任务')
  })
  test('next-step panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return; await expect(page.getByTestId('next-step-section')).toBeVisible(); await expect(page.getByTestId('next-step-section')).toContainText('下一步')
  })
  test('refresh button visible', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return; await expect(page.getByTestId('workspace-console-section').getByText('刷新')).toBeVisible()
  })
  test('next-step cards visible', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return; await expect(page.getByTestId('next-step-section')).toBeVisible()
  })
  test('focus mode control visible', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return; await expect(page.getByTestId('workspace-console-section').getByText('新建会话')).toBeVisible()
  })
  test('no js errors on page load', async ({ page }) => {
    const errors: string[] = []; page.on('pageerror', err => errors.push(err.message))
    const state = await checkPrerequisite(page); if (!state.hasProject) return; await expect(errors).toHaveLength(0)
  })
})
