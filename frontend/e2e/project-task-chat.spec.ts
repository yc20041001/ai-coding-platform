import { test, expect } from '@playwright/test'

const ADMIN_EMAIL = 'admin@example.com'
const ADMIN_PASSWORD = 'Admin@123456'
const PROJECT_NAME = `E2E-Project-${Date.now()}`

async function login(page: ReturnType<typeof test['info']>['page']) {
  await page.goto('/login')
  await page.fill('[data-testid="login-email"]', ADMIN_EMAIL)
  await page.fill('[data-testid="login-password"]', ADMIN_PASSWORD)
  await page.click('[data-testid="login-submit"]')
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
}

test.describe('Project, Task & Chat', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should create a project', async ({ page }) => {
    // Navigate to projects via URL directly
    await page.goto('/projects')
    await expect(page).toHaveURL(/\/projects/)

    // Click create button
    await page.click('[data-testid="btn-create-project"]')
    await expect(page.locator('.el-dialog')).toBeVisible({ timeout: 3000 })

    // Fill form
    const dialog = page.locator('.el-dialog')
    const input = dialog.locator('.el-input__inner').first()
    await input.fill(PROJECT_NAME)

    // Submit
    await dialog.locator('.el-button--primary').filter({ hasText: /Create|Save|Confirm|创|确|保/ }).click()

    // Wait for dialog to close and table to update
    await expect(page.locator('.el-dialog')).not.toBeVisible({ timeout: 8000 })
    await expect(page.locator('.el-table__body')).toContainText(PROJECT_NAME, { timeout: 5000 })
  })

  test('should navigate to project and view tasks tab', async ({ page }) => {
    // Navigate to projects list
    await page.goto('/projects')
    await expect(page).toHaveURL(/\/projects/)

    // Click on first project row
    const firstRow = page.locator('.el-table__body tr').first()
    await firstRow.click()
    // Project detail default tab is 'overview' at /projects/:id
    await expect(page).toHaveURL(/\/projects\/\d+/, { timeout: 5000 })

    // Click Tasks tab
    const tasksTab = page.locator('[role="tab"]:has-text("Tasks")')
    if (await tasksTab.isVisible()) {
      await tasksTab.click()
      await expect(page).toHaveURL(/\/projects\/\d+\/tasks/, { timeout: 5000 })
    }
  })

  test('should open chat tab if sessions exist', async ({ page }) => {
    // Navigate to first project
    await page.goto('/projects')
    await expect(page).toHaveURL(/\/projects/)

    const firstRow = page.locator('.el-table__body tr').first()
    await firstRow.click()
    await expect(page).toHaveURL(/\/projects\/\d+/, { timeout: 5000 })

    // Switch to Chat tab
    const chatTab = page.locator('[role="tab"]:has-text("Chat")')
    if (await chatTab.isVisible()) {
      await chatTab.click()
      await page.waitForTimeout(1000)
    }
  })
})
