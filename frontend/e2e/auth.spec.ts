import { test, expect } from '@playwright/test'

const ADMIN_EMAIL = 'admin@example.com'
const ADMIN_PASSWORD = 'Admin@123456'

test.describe('Auth', () => {
  test('should redirect to login page when unauthenticated', async ({ page }) => {
    await page.goto('/')
    await expect(page).toHaveURL(/\/login/)
  })

  test('should login successfully with admin credentials', async ({ page }) => {
    await page.goto('/login')

    await page.fill('[data-testid="login-email"]', ADMIN_EMAIL)
    await page.fill('[data-testid="login-password"]', ADMIN_PASSWORD)
    await page.click('[data-testid="login-submit"]')

    await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
    await expect(page.locator('.app-shell')).toBeVisible()
    await expect(page.locator('.tcb-user')).toContainText('admin')
  })

  test('should logout successfully', async ({ page }) => {
    await page.goto('/login')
    await page.fill('[data-testid="login-email"]', ADMIN_EMAIL)
    await page.fill('[data-testid="login-password"]', ADMIN_PASSWORD)
    await page.click('[data-testid="login-submit"]')
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })

    await page.click('[data-testid="btn-logout"]')
    await expect(page).toHaveURL(/\/login/)
  })

  test('should stay on login page with wrong password', async ({ page }) => {
    await page.goto('/login')

    await page.fill('[data-testid="login-email"]', ADMIN_EMAIL)
    await page.fill('[data-testid="login-password"]', 'wrong-password')
    await page.click('[data-testid="login-submit"]')

    // Should stay on login page after failed login
    await page.waitForTimeout(2000)
    await expect(page).toHaveURL(/\/login/)
  })
})
