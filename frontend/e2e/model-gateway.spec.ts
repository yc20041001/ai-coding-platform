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

test.describe('Model Gateway', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should navigate to model gateway page as admin', async ({ page }) => {
    await page.goto('/model-gateway')
    await expect(page).toHaveURL(/\/model-gateway/)

    // Page title should be visible regardless of API state
    await expect(page.getByText('Model Gateway').first()).toBeVisible({ timeout: 5000 })

    // Config section should be visible
    await expect(page.getByText('Model Configurations').first()).toBeVisible({ timeout: 5000 })
  })

  test('should show providers section', async ({ page }) => {
    await page.goto('/model-gateway')
    await expect(page.getByText('Available Providers').first()).toBeVisible({ timeout: 5000 })
  })
})
