import { test, expect } from '@playwright/test'

const ADMIN_EMAIL = 'admin@example.com'
const ADMIN_PASSWORD = 'Admin@123456'

async function login(page: ReturnType<typeof test['info']>['page']) {
  await page.goto('/login')
  await page.fill('[data-testid="login-email"]', ADMIN_EMAIL)
  await page.fill('[data-testid="login-password"]', ADMIN_PASSWORD)
  await page.click('[data-testid="login-submit"]')
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
}

test.describe('Knowledge & Observability', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should navigate to knowledge tab', async ({ page }) => {
    await page.goto('/projects')
    await expect(page).toHaveURL(/\/projects/)

    const firstRow = page.locator('.el-table__body tr').first()
    await firstRow.click()
    await expect(page).toHaveURL(/\/projects\/\d+/, { timeout: 5000 })

    // Switch to Knowledge tab
    const knowledgeTab = page.locator('[role="tab"]:has-text("Knowledge")')
    if (await knowledgeTab.isVisible()) {
      await knowledgeTab.click()
      await expect(page).toHaveURL(/\/projects\/\d+\/knowledge/, { timeout: 5000 })
    }
  })

  test('should perform RAG search if elements exist', async ({ page }) => {
    // Navigate to first project's knowledge tab via URL
    await page.goto('/projects')
    await expect(page).toHaveURL(/\/projects/)

    const firstRow = page.locator('.el-table__body tr').first()
    await firstRow.click()

    const knowledgeTab = page.locator('[role="tab"]:has-text("Knowledge")')
    if (await knowledgeTab.isVisible()) {
      await knowledgeTab.click()
      await page.waitForTimeout(1000)
    }

    // Try RAG search
    const searchInput = page.locator('input[placeholder*="搜索"]')
    if (await searchInput.isVisible({ timeout: 2000 })) {
      await searchInput.fill('Agent Orchestrator')
      await page.keyboard.press('Enter')
      await page.waitForTimeout(2000)
    }
  })

  test('should access observability page as admin', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    await page.waitForTimeout(2000)
    const pageContent = page.locator('.page-container, .el-card, .el-skeleton').first()
    await expect(pageContent).toBeVisible({ timeout: 5000 })
  })
})
