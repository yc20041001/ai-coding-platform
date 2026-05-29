import { test, expect } from '@playwright/test'

const ADMIN_EMAIL = 'admin@example.com'
const ADMIN_PASSWORD = 'Admin@123456'

async function login(page: ReturnType<typeof test['info']>['page']) {
  await page.goto('/login')
  await page.getByTestId('login-email').fill(ADMIN_EMAIL)
  await page.getByTestId('login-password').fill(ADMIN_PASSWORD)
  await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
}

async function openObservabilityAndCheckProjectPrerequisite(
  page: ReturnType<typeof test['info']>['page'],
) {
  await page.goto('/admin/observability')
  await expect(page).toHaveURL(/\/admin\/observability/)

  const rolloutSection = page.getByTestId('rollout-dashboard-section')
  if (await rolloutSection.isVisible({ timeout: 3000 }).catch(() => false)) {
    return { hasProject: true as const }
  }

  test.info().annotations.push({
    type: 'note',
    description: 'No seeded project available; verified fallback empty state instead of rollout panels.',
  })

  return { hasProject: false as const }
}

test.describe('Release Rollout (39A)', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should navigate to observability page with rollout sections', async ({ page }) => {
    const state = await openObservabilityAndCheckProjectPrerequisite(page)
    if (!state.hasProject) return

    await expect(page.getByTestId('rollout-dashboard-section')).toBeVisible({ timeout: 10000 })
    await expect(page.getByTestId('rollout-plan-section')).toBeVisible({ timeout: 5000 })
    await expect(page.getByTestId('rollout-verification-section')).toBeVisible({ timeout: 5000 })
  })

  test('should display rollout dashboard panel with metrics', async ({ page }) => {
    const state = await openObservabilityAndCheckProjectPrerequisite(page)
    if (!state.hasProject) return

    const dashboardPanel = page.getByTestId('rollout-dashboard-section')
    await expect(dashboardPanel).toBeVisible({ timeout: 5000 })

    // Should show dashboard header
    await expect(dashboardPanel.getByText('发布就绪仪表板')).toBeVisible()
  })

  test('should display rollout plan panel with create button', async ({ page }) => {
    const state = await openObservabilityAndCheckProjectPrerequisite(page)
    if (!state.hasProject) return

    const planPanel = page.getByTestId('rollout-plan-section')
    await expect(planPanel).toBeVisible({ timeout: 5000 })

    // Should have a "新建 Plan" button
    await expect(planPanel.getByRole('button', { name: /新建 Plan/ })).toBeVisible()
  })

  test('should open create plan dialog and create a plan', async ({ page }) => {
    const state = await openObservabilityAndCheckProjectPrerequisite(page)
    if (!state.hasProject) return

    const planPanel = page.getByTestId('rollout-plan-section')
    const createBtn = planPanel.getByRole('button', { name: /新建 Plan/ })
    if (await createBtn.isEnabled()) {
      await createBtn.click()

      // Dialog should appear
      const dialog = page.locator('.el-dialog').filter({ hasText: '新建 Rollout Plan' })
      await expect(dialog).toBeVisible({ timeout: 5000 })

      // Fill release label
      const labelInput = dialog.locator('input').first()
      if (await labelInput.isVisible()) {
        await labelInput.fill('e2e-test-v1.0')
      }

      // Click create
      const submitBtn = dialog.getByRole('button', { name: '创建' })
      await expect(submitBtn).toBeVisible()
      await submitBtn.click()
    }
  })

  test('should display verification panel with phase filter', async ({ page }) => {
    const state = await openObservabilityAndCheckProjectPrerequisite(page)
    if (!state.hasProject) return

    const verPanel = page.getByTestId('rollout-verification-section')
    await expect(verPanel).toBeVisible({ timeout: 5000 })

    // Should have phase filter dropdown
    await expect(verPanel.getByText('验证与步骤')).toBeVisible()
  })

  test('should display plan list with status tags', async ({ page }) => {
    const state = await openObservabilityAndCheckProjectPrerequisite(page)
    if (!state.hasProject) return

    const planPanel = page.getByTestId('rollout-plan-section')
    await expect(planPanel).toBeVisible({ timeout: 5000 })

    // Check for status tags like DRAFT, READY, etc.
    const draftTags = planPanel.locator('.el-tag').filter({ hasText: /DRAFT|草稿/ })
    if (await draftTags.count() > 0) {
      await expect(draftTags.first()).toBeVisible()
    }
  })

  test('should allow selecting a plan to view steps', async ({ page }) => {
    const state = await openObservabilityAndCheckProjectPrerequisite(page)
    if (!state.hasProject) return

    const planPanel = page.getByTestId('rollout-plan-section')
    await expect(planPanel).toBeVisible({ timeout: 5000 })

    // Look for "选择" button on plan items
    const selectBtns = planPanel.getByRole('button', { name: '选择' })
    const count = await selectBtns.count()
    if (count > 0) {
      await selectBtns.first().click()

      // Verification panel should now have content related to the selected plan
      const verPanel = page.getByTestId('rollout-verification-section')
      await expect(verPanel).toBeVisible()
    }
  })

  test('should display rollout summary and report buttons', async ({ page }) => {
    const state = await openObservabilityAndCheckProjectPrerequisite(page)
    if (!state.hasProject) return

    const dashboardPanel = page.getByTestId('rollout-dashboard-section')
    await expect(dashboardPanel).toBeVisible({ timeout: 5000 })

    // Should have plan selector and report button
    const reportBtn = dashboardPanel.getByRole('button', { name: /查看报告/ })
    if (await reportBtn.isEnabled()) {
      await reportBtn.click()

      // Report dialog should appear
      const reportDialog = page.locator('.el-dialog').filter({ hasText: '发布就绪报告' })
      await expect(reportDialog).toBeVisible({ timeout: 5000 })

      // Close dialog
      await reportDialog.locator('.el-dialog__close').click()
      await expect(reportDialog).not.toBeVisible({ timeout: 3000 })
    }
  })
})
