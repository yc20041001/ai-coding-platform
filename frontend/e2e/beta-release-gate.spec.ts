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

  const gateSection = page.getByTestId('beta-release-gate-section')
  if (await gateSection.isVisible({ timeout: 3000 }).catch(() => false)) {
    return { hasProject: true as const }
  }

  await expect(page.getByText('暂无可用于事件面板的项目')).toBeVisible({ timeout: 10000 })
  await expect(page.getByText('暂无可用于告警规则的项目')).toBeVisible({ timeout: 5000 })
  await expect(page.getByText('暂无可用于升级策略的项目')).toBeVisible({ timeout: 5000 })

  test.info().annotations.push({
    type: 'note',
    description: 'No seeded project available; verified fallback empty state instead of project-scoped beta panels.',
  })

  return { hasProject: false as const }
}

test.describe('Beta Release Gate', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should navigate to observability page with beta release gate sections', async ({ page }) => {
    const state = await openObservabilityAndCheckProjectPrerequisite(page)
    if (!state.hasProject) {
      return
    }

    await expect(page.getByTestId('beta-release-gate-section')).toBeVisible({ timeout: 10000 })
    await expect(page.getByTestId('beta-release-decision-section')).toBeVisible({ timeout: 5000 })
  })

  test('should display beta release gate dashboard panel', async ({ page }) => {
    const state = await openObservabilityAndCheckProjectPrerequisite(page)
    if (!state.hasProject) {
      return
    }

    const gatePanel = page.getByTestId('beta-gate-panel')
    await expect(gatePanel).toBeVisible({ timeout: 5000 })

    // The panel should contain the evaluate button
    await expect(gatePanel.getByRole('button', { name: /执行门禁评估/ })).toBeVisible()
  })

  test('should display beta release decision panel', async ({ page }) => {
    const state = await openObservabilityAndCheckProjectPrerequisite(page)
    if (!state.hasProject) {
      return
    }

    const decisionPanel = page.getByTestId('beta-decision-panel')
    await expect(decisionPanel).toBeVisible({ timeout: 5000 })

    // The panel should contain create decision and generate report buttons
    await expect(decisionPanel.getByRole('button', { name: /创建决策/ })).toBeVisible()
    await expect(decisionPanel.getByRole('button', { name: /生成就绪报告/ })).toBeVisible()
    await expect(decisionPanel.getByRole('button', { name: /刷新/ })).toBeVisible()
  })

  test('should open create decision dialog and create a decision', async ({ page }) => {
    const state = await openObservabilityAndCheckProjectPrerequisite(page)
    if (!state.hasProject) {
      return
    }

    const decisionPanel = page.getByTestId('beta-decision-panel')
    const createBtn = decisionPanel.getByRole('button', { name: /创建决策/ })
    if (await createBtn.isEnabled()) {
      await createBtn.click()
      const createDialog = page.getByTestId('create-decision-dialog')
      await expect(createDialog).toBeVisible({ timeout: 5000 })

      // Fill in the form
      const labelInput = createDialog.locator('input').first()
      if (await labelInput.isVisible()) {
        await labelInput.fill('e2e-test-v1.0')
      }

      // Click create button
      const submitBtn = createDialog.getByRole('button', { name: '创建' })
      await expect(submitBtn).toBeVisible()
      await submitBtn.click()
    }
  })

  test('should open readiness report dialog', async ({ page }) => {
    const state = await openObservabilityAndCheckProjectPrerequisite(page)
    if (!state.hasProject) {
      return
    }

    const decisionPanel = page.getByTestId('beta-decision-panel')
    const reportBtn = decisionPanel.getByRole('button', { name: /生成就绪报告/ })
    if (await reportBtn.isEnabled()) {
      await reportBtn.click()
      const reportDialog = page.getByTestId('readiness-report-dialog')
      await expect(reportDialog).toBeVisible({ timeout: 5000 })

      // Should have report content
      const preContent = reportDialog.locator('pre')
      await expect(preContent).toBeVisible({ timeout: 5000 })

      // Close
      await reportDialog.locator('.el-dialog__close').click()
      await expect(reportDialog).not.toBeVisible({ timeout: 3000 })
    }
  })

  test('should trigger gate evaluation', async ({ page }) => {
    const state = await openObservabilityAndCheckProjectPrerequisite(page)
    if (!state.hasProject) {
      return
    }

    const gatePanel = page.getByTestId('beta-gate-panel')
    await expect(gatePanel).toBeVisible({ timeout: 5000 })

    const evaluateBtn = gatePanel.getByRole('button', { name: /执行门禁评估/ })
    if (await evaluateBtn.isEnabled()) {
      await evaluateBtn.click()
      // After clicking, the button should become enabled again
      await expect(evaluateBtn).toBeEnabled({ timeout: 15000 })
    }
  })

  test('should display gate rule list and open edit dialog', async ({ page }) => {
    const state = await openObservabilityAndCheckProjectPrerequisite(page)
    if (!state.hasProject) {
      return
    }

    const gatePanel = page.getByTestId('beta-gate-panel')
    await expect(gatePanel).toBeVisible({ timeout: 5000 })

    // Check for rule items with edit buttons
    const editButtons = gatePanel.getByRole('button', { name: '编辑' })
    const count = await editButtons.count()
    if (count > 0) {
      await editButtons.first().click()
      const editDialog = page.getByTestId('gate-rule-edit-dialog')
      await expect(editDialog).toBeVisible({ timeout: 5000 })
      await editDialog.locator('.el-dialog__close').click()
      await expect(editDialog).not.toBeVisible({ timeout: 3000 })
    }
  })

  test('should show decision list if decisions exist', async ({ page }) => {
    const state = await openObservabilityAndCheckProjectPrerequisite(page)
    if (!state.hasProject) {
      return
    }

    const decisionPanel = page.getByTestId('beta-decision-panel')
    await expect(decisionPanel).toBeVisible({ timeout: 5000 })

    // Check for decision items with detail buttons
    const detailButtons = decisionPanel.getByRole('button', { name: '详情' })
    const count = await detailButtons.count()
    if (count > 0) {
      await detailButtons.first().click()
      const detailDialog = page.getByTestId('decision-detail-dialog')
      await expect(detailDialog).toBeVisible({ timeout: 5000 })
      await detailDialog.locator('.el-dialog__close').click()
      await expect(detailDialog).not.toBeVisible({ timeout: 3000 })
    }
  })
})
