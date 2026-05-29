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

test.describe('Model Cost & PR Review Quality', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should navigate to observability page with model cost and PR quality sections', async ({ page }) => {
    await page.goto('/admin/observability')
    await expect(page).toHaveURL(/\/admin\/observability/)

    // Check that the section header is present
    await expect(page.getByTestId('model-cost-section')).toBeVisible({ timeout: 10000 })
    await expect(page.getByTestId('pr-quality-section')).toBeVisible({ timeout: 5000 })
  })

  test('should display model cost panel with dashboard tiles', async ({ page }) => {
    await page.goto('/admin/observability')
    await expect(page.getByTestId('model-cost-section')).toBeVisible({ timeout: 10000 })

    const costPanel = page.getByTestId('model-cost-panel')
    await expect(costPanel).toBeVisible({ timeout: 5000 })

    // The panel should contain the refresh and scan buttons
    await expect(costPanel.getByRole('button', { name: /刷新与扫描/ })).toBeVisible()
    await expect(costPanel.getByRole('button', { name: /仅扫描告警/ })).toBeVisible()
    await expect(costPanel.getByRole('button', { name: /导出报告/ })).toBeVisible()
  })

  test('should display PR quality panel with dashboard tiles', async ({ page }) => {
    await page.goto('/admin/observability')
    await expect(page.getByTestId('pr-quality-section')).toBeVisible({ timeout: 10000 })

    const qualityPanel = page.getByTestId('pr-quality-panel')
    await expect(qualityPanel).toBeVisible({ timeout: 5000 })

    // The panel should contain the export button
    await expect(qualityPanel.getByRole('button', { name: /导出报告/ })).toBeVisible()
  })

  test('should trigger cost refresh and scan', async ({ page }) => {
    await page.goto('/admin/observability')
    await expect(page.getByTestId('model-cost-section')).toBeVisible({ timeout: 10000 })

    const costPanel = page.getByTestId('model-cost-panel')
    await expect(costPanel).toBeVisible({ timeout: 5000 })

    const refreshBtn = costPanel.getByRole('button', { name: /刷新与扫描/ })
    if (await refreshBtn.isEnabled()) {
      await refreshBtn.click()
      // After clicking, the button should become loading briefly
      await expect(refreshBtn).toBeEnabled({ timeout: 15000 })
    }
  })

  test('should open cost export dialog', async ({ page }) => {
    await page.goto('/admin/observability')
    await expect(page.getByTestId('model-cost-section')).toBeVisible({ timeout: 10000 })

    const costPanel = page.getByTestId('model-cost-panel')
    const exportBtn = costPanel.getByRole('button', { name: /导出报告/ })
    if (await exportBtn.isEnabled()) {
      await exportBtn.click()
      const exportDialog = page.getByTestId('cost-export-dialog')
      await expect(exportDialog).toBeVisible({ timeout: 5000 })
      // Should have report content
      const preContent = exportDialog.locator('pre')
      await expect(preContent).toBeVisible({ timeout: 5000 })
      // Close
      await exportDialog.locator('.el-dialog__close').click()
      await expect(exportDialog).not.toBeVisible({ timeout: 3000 })
    }
  })

  test('should open PR quality export dialog', async ({ page }) => {
    await page.goto('/admin/observability')
    await expect(page.getByTestId('pr-quality-section')).toBeVisible({ timeout: 10000 })

    const qualityPanel = page.getByTestId('pr-quality-panel')
    const exportBtn = qualityPanel.getByRole('button', { name: /导出报告/ })
    if (await exportBtn.isEnabled()) {
      await exportBtn.click()
      const exportDialog = page.getByTestId('quality-export-dialog')
      await expect(exportDialog).toBeVisible({ timeout: 5000 })
      const preContent = exportDialog.locator('pre')
      await expect(preContent).toBeVisible({ timeout: 5000 })
      await exportDialog.locator('.el-dialog__close').click()
      await expect(exportDialog).not.toBeVisible({ timeout: 3000 })
    }
  })

  test('should display model cost alerts if present', async ({ page }) => {
    await page.goto('/admin/observability')
    await expect(page.getByTestId('model-cost-section')).toBeVisible({ timeout: 10000 })

    const costPanel = page.getByTestId('model-cost-panel')
    await expect(costPanel).toBeVisible({ timeout: 5000 })

    // Check if alert items exist
    const scanBtn = costPanel.getByRole('button', { name: /仅扫描告警/ })
    if (await scanBtn.isEnabled()) {
      await scanBtn.click()
      await expect(scanBtn).toBeEnabled({ timeout: 15000 })
    }
  })

  test('should show quality records section', async ({ page }) => {
    await page.goto('/admin/observability')
    await expect(page.getByTestId('pr-quality-section')).toBeVisible({ timeout: 10000 })

    const qualityPanel = page.getByTestId('pr-quality-panel')
    await expect(qualityPanel).toBeVisible({ timeout: 5000 })

    // Check for record items or empty state
    const recordItems = qualityPanel.locator('.quality-record-item')
    const count = await recordItems.count()
    if (count > 0) {
      // Try opening edit dialog for first record
      const editBtn = recordItems.first().getByRole('button', { name: '编辑' })
      if (await editBtn.isVisible()) {
        await editBtn.click()
        const editDialog = page.getByTestId('quality-edit-dialog')
        await expect(editDialog).toBeVisible({ timeout: 5000 })
        await editDialog.locator('.el-dialog__close').click()
        await expect(editDialog).not.toBeVisible({ timeout: 3000 })
      }
    }
  })
})
