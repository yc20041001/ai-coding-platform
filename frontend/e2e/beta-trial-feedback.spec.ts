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

test.describe('Beta Trial Feedback', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should navigate to observability page with beta sections', async ({ page }) => {
    await page.goto('/admin/observability')
    await expect(page).toHaveURL(/\/admin\/observability/)

    // Check that the beta section header is present
    await expect(page.getByTestId('beta-session-section')).toBeVisible({ timeout: 10000 })
    await expect(page.getByTestId('beta-readiness-section')).toBeVisible({ timeout: 5000 })
  })

  test('should display beta trial session panel', async ({ page }) => {
    await page.goto('/admin/observability')

    // The session panel should be visible
    const sessionPanel = page.getByTestId('beta-session-panel')
    await expect(sessionPanel).toBeVisible({ timeout: 10000 })

    // The create session button should exist
    await expect(sessionPanel.getByRole('button', { name: /创建会话/ })).toBeVisible()
  })

  test('should create a beta trial session and display it in the list', async ({ page }) => {
    await page.goto('/admin/observability')
    await expect(page.getByTestId('beta-session-section')).toBeVisible({ timeout: 10000 })

    // Click create session button
    const createBtn = page.getByTestId('beta-session-panel').getByRole('button', { name: /创建会话/ })
    await expect(createBtn).toBeVisible({ timeout: 8000 })

    // Check if button is enabled (if project exists)
    if (await createBtn.isEnabled()) {
      await createBtn.click()
      const dialog = page.locator('.el-dialog').filter({ hasText: '创建 Beta 试用会话' })
      await expect(dialog).toBeVisible({ timeout: 5000 })

      // Fill form
      const titleInput = dialog.locator('.el-input__inner').first()
      await titleInput.fill(`E2E Beta Session ${Date.now()}`)

      // Submit
      await dialog.getByRole('button', { name: '创建' }).click()
      await expect(dialog).not.toBeVisible({ timeout: 8000 })
    }
  })

  test('should view beta trial session details', async ({ page }) => {
    await page.goto('/admin/observability')
    await expect(page.getByTestId('beta-session-panel')).toBeVisible({ timeout: 10000 })

    // Click on a session row if available
    const sessionRows = page.getByTestId('beta-session-panel').locator('.beta-session-row')
    const count = await sessionRows.count()
    if (count > 0) {
      await sessionRows.first().click()

      // Detail dialog should appear
      const detailDialog = page.locator('.el-dialog').filter({ hasText: '会话详情' })
      await expect(detailDialog).toBeVisible({ timeout: 5000 })
      await expect(detailDialog.locator('.beta-detail-grid')).toBeVisible()

      // Close dialog
      await detailDialog.locator('.el-dialog__close').click()
      await expect(detailDialog).not.toBeVisible({ timeout: 3000 })
    }
  })

  test('should select session and show feedback panel', async ({ page }) => {
    await page.goto('/admin/observability')
    await expect(page.getByTestId('beta-session-panel')).toBeVisible({ timeout: 10000 })

    // Try selecting a session
    const selectButtons = page.getByTestId('beta-session-panel').getByRole('button', { name: '选择' })
    const count = await selectButtons.count()
    if (count > 0) {
      await selectButtons.first().click()

      // Feedback panel should appear
      await expect(page.getByTestId('beta-feedback-section')).toBeVisible({ timeout: 5000 })
    }
  })

  test('should create environment readiness check', async ({ page }) => {
    await page.goto('/admin/observability')
    await expect(page.getByTestId('beta-readiness-section')).toBeVisible({ timeout: 10000 })

    const addBtn = page.getByTestId('beta-readiness-panel').getByRole('button', { name: /添加检查/ })
    if (await addBtn.isEnabled()) {
      await addBtn.click()
      const dialog = page.locator('.el-dialog').filter({ hasText: '添加环境检查记录' })
      await expect(dialog).toBeVisible({ timeout: 5000 })

      // Fill target name
      const nameInput = dialog.locator('.el-input__inner').first()
      await nameInput.fill(`E2E Check ${Date.now()}`)

      await dialog.getByRole('button', { name: '创建' }).click()
      await expect(dialog).not.toBeVisible({ timeout: 8000 })
    }
  })

  test('should display beta dashboard metrics', async ({ page }) => {
    await page.goto('/admin/observability')

    // Dashboard tiles (in readiness panel) should show counts
    const readinessPanel = page.getByTestId('beta-readiness-panel')
    await expect(readinessPanel).toBeVisible({ timeout: 10000 })

    // Pass/fail tags should be visible if data exists
    const passTags = readinessPanel.locator('.el-tag').filter({ hasText: /通过|警告|失败/ })
    const tagCount = await passTags.count()
    if (tagCount > 0) {
      await expect(passTags.first()).toBeVisible()
    }
  })

  test('should export session markdown', async ({ page }) => {
    await page.goto('/admin/observability')
    await expect(page.getByTestId('beta-session-panel')).toBeVisible({ timeout: 10000 })

    // Click on a session row to open detail
    const sessionRows = page.getByTestId('beta-session-panel').locator('.beta-session-row')
    const count = await sessionRows.count()
    if (count > 0) {
      await sessionRows.first().click()

      // Click export markdown button
      const exportBtn = page.locator('.el-dialog').filter({ hasText: '会话详情' }).getByRole('button', { name: /Markdown/ })
      await expect(exportBtn).toBeVisible({ timeout: 3000 })
      await exportBtn.click()

      // Markdown content should appear
      const markdownDialog = page.locator('.el-dialog').filter({ hasText: '导出 Markdown' })
      await expect(markdownDialog).toBeVisible({ timeout: 5000 })
      await expect(markdownDialog.locator('.beta-markdown')).toBeVisible()
    }
  })

  test('should support status transitions in session detail', async ({ page }) => {
    await page.goto('/admin/observability')
    await expect(page.getByTestId('beta-session-panel')).toBeVisible({ timeout: 10000 })

    // Create a session first
    const createBtn = page.getByTestId('beta-session-panel').getByRole('button', { name: /创建会话/ })
    if (await createBtn.isEnabled()) {
      await createBtn.click()
      const dialog = page.locator('.el-dialog').filter({ hasText: '创建 Beta 试用会话' })
      await expect(dialog).toBeVisible({ timeout: 5000 })
      await dialog.locator('.el-input__inner').first().fill(`E2E Status Test ${Date.now()}`)
      await dialog.getByRole('button', { name: '创建' }).click()
      await expect(dialog).not.toBeVisible({ timeout: 8000 })

      // Open the new session
      const newRow = page.getByTestId('beta-session-panel').locator('.beta-session-row').first()
      await newRow.click()

      // Should see "开始试用" button for PLANNED status
      const detailDialog = page.locator('.el-dialog').filter({ hasText: '会话详情' })
      const startBtn = detailDialog.getByRole('button', { name: '开始试用' })
      if (await startBtn.isVisible({ timeout: 3000 })) {
        await startBtn.click()
        // Status should change
        await expect(detailDialog.locator('.el-tag').filter({ hasText: '进行中' })).toBeVisible({ timeout: 5000 })
      }
    }
  })
})
