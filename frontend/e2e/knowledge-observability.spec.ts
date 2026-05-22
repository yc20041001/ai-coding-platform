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

test.describe('Knowledge & Observability', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should navigate to knowledge tab', async ({ page }) => {
    // Navigate to projects list, then first project
    await page.goto('/projects')
    await expect(page).toHaveURL(/\/projects/)

    // The projects page container should always be visible
    await expect(page.locator('.page-container')).toBeVisible({ timeout: 8000 })

    // Use the first project row if available, otherwise skip navigation
    const tableArea = page.getByTestId('project-table-area')
    const tableRows = page.getByTestId('project-table')
    if (await tableRows.isVisible({ timeout: 3000 })) {
      const firstRow = tableRows.locator('tr').nth(1)
      await firstRow.click()
      await expect(page).toHaveURL(/\/projects\/\d+/, { timeout: 8000 })

      // Navigate to Knowledge tab via URL
      const projectId = page.url().split('/').pop()
      await page.goto(`/projects/${projectId}/knowledge`)
      await expect(page).toHaveURL(/\/projects\/\d+\/knowledge/)
      await expect(page.locator('.page-container')).toBeVisible({ timeout: 5000 })
    }
  })

  test('should perform RAG search if elements exist', async ({ page }) => {
    // Navigate to first project's knowledge tab via URL
    await page.goto('/projects')
    await expect(page).toHaveURL(/\/projects/)

    const tableRows = page.getByTestId('project-table')
    if (await tableRows.isVisible({ timeout: 3000 })) {
      const firstRow = tableRows.locator('tr').nth(1)
      await firstRow.click()
      const projectId = page.url().split('/').pop()
      await page.goto(`/projects/${projectId}/knowledge`)

      // Try RAG search if search input is available
      const searchInput = page.locator('input[placeholder*="search" i], input[placeholder*="Search"]')
      if (await searchInput.isVisible({ timeout: 3000 })) {
        await searchInput.fill('Agent')
        await page.keyboard.press('Enter')
        await expect(page.locator('.page-container')).toBeVisible({ timeout: 8000 })
      }
    }
  })

  test('should access observability page as admin', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    // Observability page should render
    await expect(page.locator('.page-container')).toBeVisible({ timeout: 8000 })
  })

  test('should show tool execution metrics panel on observability page', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    // Tool worker metrics panel should be visible
    const metricsPanel = page.getByTestId('tool-metrics-panel')
    await expect(metricsPanel).toBeVisible({ timeout: 10000 })
  })

  test('should render tool metrics summary cards', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    // Summary cards should render within the panel
    const summaryGrid = page.getByTestId('tool-metrics-summary')
    await expect(summaryGrid).toBeVisible({ timeout: 10000 })
  })

  test('should render tool metrics table or empty state', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    // Either the tool metrics table is visible or empty state is shown
    const table = page.getByTestId('tool-metrics-table')
    const emptyState = page.getByTestId('tool-metrics-panel').locator('.el-empty')
    await expect(table.or(emptyState)).toBeVisible({ timeout: 10000 })
  })

  test('should render failure metrics section', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    // Failure metrics section should be present (may be empty)
    const failuresSection = page.getByTestId('tool-metrics-failures')
    await expect(failuresSection).toBeVisible({ timeout: 10000 })
  })

  test('should render daily trend section', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    // Daily trend section should be present (may be empty)
    const dailySection = page.getByTestId('tool-metrics-daily')
    await expect(dailySection).toBeVisible({ timeout: 10000 })
  })

  test('should have no page-level JavaScript errors', async ({ page }) => {
    const jsErrors: string[] = []
    page.on('pageerror', (err) => jsErrors.push(err.message))

    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)
    await expect(page.locator('.page-container')).toBeVisible({ timeout: 8000 })

    // Wait for panels to load
    await page.waitForTimeout(2000)

    expect(jsErrors).toHaveLength(0)
  })

  test('should show problem jobs section on observability page', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    // Problem jobs section should be present may be empty
    const failuresSection = page.getByTestId('tool-metrics-failures')
    await expect(failuresSection).toBeVisible({ timeout: 10000 })

    // The section should show "Problem Jobs" heading or empty state
    await expect(failuresSection).toContainText(/失败|Problem|Fail/, { timeout: 3000 })
  })
})
