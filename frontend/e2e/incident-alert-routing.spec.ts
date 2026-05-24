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

test.describe('Incident & Alert Routing', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should access observability page and show incident section', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    // Incident panel section should be visible
    const incidentSection = page.getByTestId('incident-section')
    await expect(incidentSection).toBeVisible({ timeout: 10000 })
  })

  test('should show alert rule and escalation policy sections on observability page', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const alertRuleSection = page.getByTestId('alert-rule-section')
    await expect(alertRuleSection).toBeVisible({ timeout: 10000 })

    const policySection = page.getByTestId('escalation-policy-section')
    await expect(policySection).toBeVisible({ timeout: 10000 })
  })

  test('should render incident panel with summary and toolbar', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const incidentPanel = page.getByTestId('tool-incident-panel')
    await expect(incidentPanel).toBeVisible({ timeout: 10000 })

    // Summary bar should be visible or empty state shown
    const summary = page.getByTestId('tip-summary')
    const emptyState = page.getByTestId('tip-empty')
    await expect(summary.or(emptyState).first()).toBeVisible({ timeout: 10000 })

    // Toolbar should have SLA scan, escalation scan, sync, and create buttons
    await expect(page.getByTestId('tip-scan-sla-btn')).toBeVisible({ timeout: 5000 })
    await expect(page.getByTestId('tip-scan-esc-btn')).toBeVisible({ timeout: 5000 })
    await expect(page.getByTestId('tip-sync-btn')).toBeVisible({ timeout: 5000 })
    await expect(page.getByTestId('tip-create-btn')).toBeVisible({ timeout: 5000 })
  })

  test('should open create incident dialog from panel', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    // Click create incident button
    await page.getByTestId('tip-create-btn').click()

    // Dialog should appear
    const dialog = page.getByTestId('incident-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Form should have required fields
    await expect(page.getByTestId('tid-source-type')).toBeVisible()
    await expect(page.getByTestId('tid-severity')).toBeVisible()
    await expect(page.getByTestId('tid-title')).toBeVisible()

    // Close dialog
    await page.getByTestId('tid-cancel-btn').click()
    await expect(dialog).not.toBeVisible({ timeout: 3000 })
  })

  test('should show alert rule panel with add button', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const rulePanel = page.getByTestId('alert-rule-panel')
    await expect(rulePanel).toBeVisible({ timeout: 10000 })

    // Add rule button should be visible
    await expect(page.getByTestId('tar-add-btn')).toBeVisible({ timeout: 5000 })
  })

  test('should open alert rule creation form', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    // Click add rule button
    await page.getByTestId('tar-add-btn').click()

    // Form should appear
    const form = page.getByTestId('tar-form')
    await expect(form).toBeVisible({ timeout: 5000 })

    // Form inputs should be visible
    await expect(page.getByTestId('tar-name')).toBeVisible()
    await expect(page.getByTestId('tar-target')).toBeVisible()

    // Close by cancel
    await page.getByTestId('tar-form').locator('button', { hasText: '取消' }).click()
    await expect(form).not.toBeVisible({ timeout: 3000 })
  })

  test('should have no page-level JavaScript errors on observability page', async ({ page }) => {
    const jsErrors: string[] = []
    page.on('pageerror', (err) => jsErrors.push(err.message))

    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)
    await expect(page.locator('.page-container')).toBeVisible({ timeout: 8000 })

    // Wait for panels to load
    await page.waitForTimeout(2000)

    expect(jsErrors).toHaveLength(0)
  })

  test('should render incident table or empty state', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const incidentPanel = page.getByTestId('tool-incident-panel')
    await expect(incidentPanel).toBeVisible({ timeout: 10000 })

    // Either the data table is visible, or the empty state is shown
    const table = page.getByTestId('tip-table')
    const emptyState = page.getByTestId('tip-empty')
    await expect(table.or(emptyState)).toBeVisible({ timeout: 10000 })
  })
})
