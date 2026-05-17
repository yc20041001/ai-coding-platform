import { test, expect, Page } from '@playwright/test'

const ADMIN_EMAIL = 'admin@example.com'
const ADMIN_PASSWORD = 'Admin@123456'

async function fillLoginForm(page: Page, email: string, password: string) {
  await page.getByTestId('login-email').fill(email)
  await page.getByTestId('login-password').fill(password)
}

test.describe('Auth', () => {
  test('should redirect to login page when unauthenticated', async ({ page }) => {
    await page.goto('/projects')
    await expect(page).toHaveURL(/\/login/, { timeout: 10000 })
  })

  test('should show public page when unauthenticated at root', async ({ page }) => {
    await page.goto('/')
    // Root redirects to /public when unauthenticated (Milestone 26)
    await expect(page).toHaveURL(/\/public/, { timeout: 10000 })
    await expect(page.getByRole('heading', { name: 'AI Coding Platform' })).toBeVisible()
  })

  test('should login successfully with admin credentials', async ({ page }) => {
    await page.goto('/login')

    await fillLoginForm(page, ADMIN_EMAIL, ADMIN_PASSWORD)
    // Captcha is disabled in E2E test environment (AUTH_CAPTCHA_ENABLED=false)
    await page.getByTestId('login-submit').click()

    await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
    await expect(page.locator('.app-shell')).toBeVisible()
    await expect(page.locator('.tcb-user')).toContainText('admin')
  })

  test('should logout successfully', async ({ page }) => {
    await page.goto('/login')
    await fillLoginForm(page, ADMIN_EMAIL, ADMIN_PASSWORD)
    await page.getByTestId('login-submit').click()
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })

    await page.getByTestId('btn-logout').click()
    await expect(page).toHaveURL(/\/login/, { timeout: 10000 })
  })

  test('should stay on login page with wrong password', async ({ page }) => {
    await page.goto('/login')

    await fillLoginForm(page, ADMIN_EMAIL, 'wrong-password-xyz')
    await page.getByTestId('login-submit').click()

    // Submit triggers API call; wait for error alert to appear
    await expect(page.getByTestId('login-error')).toBeVisible({ timeout: 10000 })
    await expect(page).toHaveURL(/\/login/)
  })

  test('should show captcha on login page', async ({ page }) => {
    await page.goto('/login')
    // Verify captcha image appears (backend may be slow to start in CI)
    await expect(page.getByTestId('login-captcha-code')).toBeVisible({ timeout: 15000 })
  })
})
