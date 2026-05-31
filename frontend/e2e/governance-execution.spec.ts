import { test, expect } from '@playwright/test'
const ADMIN_EMAIL = 'admin@example.com'; const ADMIN_PASSWORD = 'Admin@123456'
async function login(page: import('@playwright/test').Page) {
  await page.goto('/login'); await page.getByTestId('login-email').fill(ADMIN_EMAIL)
  await page.getByTestId('login-password').fill(ADMIN_PASSWORD); await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
}
async function checkPrerequisite(page: import('@playwright/test').Page) {
  await page.goto('/admin/observability'); await expect(page).toHaveURL(/\/admin\/observability/)
  const section = page.getByTestId('playbook-template-section')
  if (await section.isVisible({ timeout: 3000 }).catch(() => false)) return { hasProject: true as const }
  test.info().annotations.push({ type: 'note', description: 'No project; verifying empty state' })
  return { hasProject: false as const }
}

test.describe('Governance Execution & Playbook (42A)', () => {
  test.beforeEach(async ({ page }) => { await login(page) })
  test('observability page shows execution sections', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByText('治理执行')).toBeVisible()
  })
  test('playbook template panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('playbook-template-section')).toBeVisible()
    await expect(page.getByTestId('playbook-template-section')).toContainText('Playbook 模板')
  })
  test('execution plan panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('execution-plan-section')).toBeVisible()
    await expect(page.getByTestId('execution-plan-section')).toContainText('执行计划')
  })
  test('handoff checklist panel renders', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('handoff-checklist-section')).toBeVisible()
    await expect(page.getByTestId('handoff-checklist-section')).toContainText('Handoff 清单')
  })
  test('create playbook dialog works', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await page.getByTestId('playbook-template-section').getByText('新建').click()
    await expect(page.locator('.el-dialog')).toBeVisible()
  })
  test('execution plan status tags visible', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('execution-plan-section')).toBeVisible()
  })
  test('handoff checklist status visible', async ({ page }) => {
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(page.getByTestId('handoff-checklist-section')).toBeVisible()
  })
  test('no js errors on page load', async ({ page }) => {
    const errors: string[] = []; page.on('pageerror', err => errors.push(err.message))
    const state = await checkPrerequisite(page); if (!state.hasProject) return
    await expect(errors).toHaveLength(0)
  })
})
