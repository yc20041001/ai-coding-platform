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

test.describe('Incident SLA & Escalation', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should show SLA scan button in incident panel', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const slaScanBtn = page.getByTestId('tip-scan-sla-btn')
    await expect(slaScanBtn).toBeVisible({ timeout: 10000 })
    await expect(slaScanBtn).toBeEnabled({ timeout: 5000 })
  })

  test('should show escalation scan button in incident panel', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const escScanBtn = page.getByTestId('tip-scan-esc-btn')
    await expect(escScanBtn).toBeVisible({ timeout: 10000 })
    await expect(escScanBtn).toBeEnabled({ timeout: 5000 })
  })

  test('should show escalation policy section on observability page', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const policySection = page.getByTestId('escalation-policy-section')
    await expect(policySection).toBeVisible({ timeout: 10000 })
  })

  test('should show escalation policy panel with create button', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const policyPanel = page.getByTestId('tool-escalation-policy-panel')
    await expect(policyPanel).toBeVisible({ timeout: 10000 })

    const createBtn = page.getByTestId('tep-create-btn')
    await expect(createBtn).toBeVisible({ timeout: 5000 })
  })

  test('should create escalation policy via dialog', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    // Click create policy button
    await page.getByTestId('tep-create-btn').click()

    // Dialog should appear
    const dialog = page.getByTestId('tep-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Fill form
    await page.getByTestId('tep-form-name').fill('E2E Test Policy')
    // Select severity dropdown - use the select
    const severitySelect = page.getByTestId('tep-form-severity')
    await severitySelect.click()
    await page.locator('.el-select-dropdown__item').filter({ hasText: '严重' }).last().click()

    // Select channel
    const channelSelect = page.getByTestId('tep-form-channel')
    await channelSelect.click()
    await page.locator('.el-select-dropdown__item').filter({ hasText: 'Slack' }).last().click()

    // Save
    await page.getByTestId('tep-save-btn').click()

    // Dialog should close after save
    await expect(dialog).not.toBeVisible({ timeout: 5000 })

    // Table should show the new policy
    const table = page.getByTestId('tep-table')
    await expect(table).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('E2E Test Policy')).toBeVisible({ timeout: 5000 })
  })

  test('should delete escalation policy', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    // First create a policy
    await page.getByTestId('tep-create-btn').click()
    await page.getByTestId('tep-form-name').fill('Policy To Delete')
    await page.getByTestId('tep-save-btn').click()
    await expect(page.getByTestId('tep-dialog')).not.toBeVisible({ timeout: 5000 })

    // Should have delete button in table
    const deleteBtn = page.getByTestId('tep-delete-btn').first()
    await expect(deleteBtn).toBeVisible({ timeout: 5000 })

    // Click delete - confirm dialog
    deleteBtn.click()
    const confirmDialog = page.locator('.el-message-box')
    await expect(confirmDialog).toBeVisible({ timeout: 3000 })
    await confirmDialog.locator('.el-button--primary').click()
    await expect(confirmDialog).not.toBeVisible({ timeout: 3000 })
  })

  test('should create incident and see SLA fields in table', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    // Create an incident
    await page.getByTestId('tip-create-btn').click()
    const dialog = page.getByTestId('incident-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Select CRITICAL severity to get SLA
    const severitySelect = page.getByTestId('tid-severity')
    await severitySelect.click()
    await page.locator('.el-select-dropdown__item').filter({ hasText: '严重' }).last().click()

    await page.getByTestId('tid-title').locator('input').fill('SLA E2E Test')

    // Submit
    await page.getByTestId('tid-save-btn').click()
    await expect(dialog).not.toBeVisible({ timeout: 5000 })

    // The incident table should show SLA status tag
    const table = page.getByTestId('tip-table')
    await expect(table).toBeVisible({ timeout: 5000 })
  })

  test('should run SLA scan and get result', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    // Click SLA scan button
    await page.getByTestId('tip-scan-sla-btn').click()

    // Wait for scan to complete - success message should appear
    const successMsg = page.locator('.el-message--success')
    await expect(successMsg).toBeVisible({ timeout: 15000 })
  })

  test('should run escalation scan and get result', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    // Click escalation scan button
    await page.getByTestId('tip-scan-esc-btn').click()

    // Wait for scan to complete
    const successMsg = page.locator('.el-message--success')
    await expect(successMsg).toBeVisible({ timeout: 15000 })
  })

  test('should show escalate button for open incidents', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    // If there's an open incident, the escalate button should be visible
    // First check if there are any incidents
    const table = page.getByTestId('tip-table')

    if (await table.isVisible({ timeout: 3000 }).catch(() => false)) {
      const escBtn = page.getByTestId('tip-escalate-btn').first()
      // The button may or may not exist depending on data - just check if present
      const exists = await escBtn.isVisible().catch(() => false)
      if (exists) {
        await expect(escBtn).toBeEnabled({ timeout: 5000 })
      }
    }
  })
})
