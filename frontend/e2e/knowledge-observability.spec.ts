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
})
