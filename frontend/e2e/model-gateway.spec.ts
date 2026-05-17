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

test.describe('Model Gateway', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should navigate to model gateway page as admin', async ({ page }) => {
    await page.goto('/model-gateway')
    await expect(page).toHaveURL(/\/model-gateway/)

    // Page title should be visible regardless of API state
    await expect(page.getByText('模型网关').first()).toBeVisible({ timeout: 8000 })
  })

  test('should show providers section', async ({ page }) => {
    await page.goto('/model-gateway')
    // Provider section should be present
    await expect(page.locator('.page-container')).toBeVisible({ timeout: 8000 })
  })
})
