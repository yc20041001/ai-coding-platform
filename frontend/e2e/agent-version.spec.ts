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

test.describe('Agent Version Management', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should open version drawer from agent list page', async ({ page }) => {
    await page.goto('/agents')
    await expect(page).toHaveURL(/\/agents/)

    // Wait for agent table to load
    await expect(page.locator('.page-container')).toBeVisible({ timeout: 8000 })

    // Click the version button on the first agent row
    const versionBtn = page.getByTestId('btn-agent-versions').first()
    await expect(versionBtn).toBeVisible({ timeout: 8000 })
    await versionBtn.click()

    // Drawer should open
    const drawer = page.getByTestId('agent-version-drawer')
    await expect(drawer).toBeVisible({ timeout: 5000 })

    // Version list should be visible
    const versionList = page.getByTestId('agent-version-list')
    await expect(versionList).toBeVisible({ timeout: 5000 })

    // At least one version item should be present
    const items = page.getByTestId('agent-version-item')
    const count = await items.count()
    expect(count).toBeGreaterThanOrEqual(1)
  })

  test('should show version detail with systemPrompt, toolPolicy, executionPolicy', async ({ page }) => {
    await page.goto('/agents')
    await expect(page).toHaveURL(/\/agents/)
    await expect(page.locator('.page-container')).toBeVisible({ timeout: 8000 })

    // Open version drawer
    const versionBtn = page.getByTestId('btn-agent-versions').first()
    await expect(versionBtn).toBeVisible({ timeout: 8000 })
    await versionBtn.click()

    const drawer = page.getByTestId('agent-version-drawer')
    await expect(drawer).toBeVisible({ timeout: 5000 })

    // Click the first version item
    const firstItem = page.getByTestId('agent-version-item').first()
    await expect(firstItem).toBeVisible({ timeout: 5000 })
    await firstItem.click()

    // Version detail panel should be visible
    const detail = page.getByTestId('agent-version-detail')
    await expect(detail).toBeVisible({ timeout: 5000 })

    // Detail section should contain systemPrompt, toolPolicy, executionPolicy sections
    // Headers are rendered with CSS text-transform:uppercase
    const detailText = await detail.innerText()
    expect(detailText).toContain('SYSTEM PROMPT')
    expect(detailText).toContain('TOOL POLICY')
    expect(detailText).toContain('EXECUTION POLICY')
  })

  test('should show published versions in project agent enable dialog', async ({ page }) => {
    // Navigate to projects and create one
    const suffix = Date.now().toString() + Math.random().toString(36).slice(2, 6)
    const projectName = `E2E-VerSel-${suffix}`

    await page.goto('/projects')
    await expect(page).toHaveURL(/\/projects/)

    await page.getByTestId('btn-create-project').click()
    const projectForm = page.getByTestId('dialog-create-project')
    await expect(projectForm).toBeVisible({ timeout: 5000 })

    await page.getByTestId('input-project-name').fill(projectName)
    await Promise.all([
      page.waitForResponse(
        resp => resp.url().includes('/api/projects') && resp.request().method() === 'POST' && resp.status() === 200,
        { timeout: 15000 },
      ),
      page.getByTestId('btn-submit-project').click(),
    ])
    await expect(projectForm).not.toBeVisible({ timeout: 8000 })

    await page.getByTestId('project-table-area').getByText(projectName).first().click()
    await expect(page).toHaveURL(/\/projects\/\d+/, { timeout: 8000 })

    const projectId = page.url().split('/').pop()!
    await page.goto(`/projects/${projectId}/agents`)
    await expect(page).toHaveURL(/\/projects\/\d+\/agents/)

    // Wait for agent table
    const agentTable = page.getByTestId('project-agent-table')
    await expect(agentTable).toBeVisible({ timeout: 10000 })

    // Click Enable button to open dialog
    const enableBtn = page.getByTestId('btn-agent-enable').first()
    await enableBtn.click()

    // Dialog should be visible
    const dialog = page.getByTestId('agent-enable-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Version select should be visible with options
    const versionSelect = page.getByTestId('select-agent-version')
    await expect(versionSelect).toBeVisible({ timeout: 5000 })

    // Click the select to open dropdown and verify options exist
    await versionSelect.click()
    const options = page.locator('.el-select-dropdown__item')
    const optionCount = await options.count()
    expect(optionCount).toBeGreaterThanOrEqual(1)

    // Close dropdown by clicking outside
    await page.locator('.el-dialog__title').click()

    // Confirm enable
    await page.getByTestId('btn-confirm-enable-agent').click()

    // Wait for table to reload (button should change to Disable)
    await expect(page.getByTestId('btn-agent-disable').first()).toBeVisible({ timeout: 10000 })
  })

  test('should have no JS errors on version drawer', async ({ page }) => {
    const jsErrors: string[] = []
    page.on('pageerror', (err) => {
      jsErrors.push(err.message)
    })

    await page.goto('/agents')
    await expect(page).toHaveURL(/\/agents/)
    await expect(page.locator('.page-container')).toBeVisible({ timeout: 8000 })

    const versionBtn = page.getByTestId('btn-agent-versions').first()
    await expect(versionBtn).toBeVisible({ timeout: 8000 })
    await versionBtn.click()

    const drawer = page.getByTestId('agent-version-drawer')
    await expect(drawer).toBeVisible({ timeout: 5000 })

    // Click version item
    const firstItem = page.getByTestId('agent-version-item').first()
    await expect(firstItem).toBeVisible({ timeout: 5000 })
    await firstItem.click()

    // No JS errors
    expect(jsErrors).toEqual([])
  })
})
