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

async function createProjectAndTask(page: ReturnType<typeof test['info']>['page']): Promise<{ projectId: string; taskId: string }> {
  const suffix = Date.now().toString() + Math.random().toString(36).slice(2, 6)
  const projectName = `E2E-WFT-${suffix}`
  const taskTitle = `E2E-WFT-Task-${suffix}`

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

  // Enable multi-agent agents for this project
  const token = await page.evaluate(() => localStorage.getItem('aicp_token'))
  const agentIds = [300001, 300002, 300003, 300004, 300005]
  for (const agentId of agentIds) {
    await page.evaluate(
      async ({ pid, aid, tok }) => {
        await fetch(`/api/projects/${pid}/agents/${aid}/enable`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${tok}` },
          body: '{}',
        })
      },
      { pid: projectId, aid: agentId, tok: token },
    )
  }

  await page.goto(`/projects/${projectId}/tasks`)
  await expect(page).toHaveURL(/\/projects\/\d+\/tasks/)

  await page.getByTestId('btn-create-task').click()
  const taskForm = page.getByTestId('dialog-create-task')
  await expect(taskForm).toBeVisible({ timeout: 5000 })

  await page.getByTestId('input-task-title').fill(taskTitle)
  await page.getByTestId('btn-submit-task').click()
  await expect(taskForm).not.toBeVisible({ timeout: 15000 })

  const taskTable = page.getByTestId('task-table')
  await expect(taskTable).toContainText(taskTitle, { timeout: 8000 })
  await taskTable.getByTestId('btn-task-detail').first().click()

  await expect(page).toHaveURL(/\/projects\/\d+\/tasks\/\d+/, { timeout: 8000 })
  const taskId = page.url().split('/').pop()!

  return { projectId, taskId }
}

test.describe('Workflow Template Management', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should display workflow templates nav entry for admin', async ({ page }) => {
    // Admin should see the workflow nav item in the floating dock
    await expect(page.getByTestId('nav-workflow-templates')).toBeVisible({ timeout: 5000 })
  })

  test('should open workflow templates page', async ({ page }) => {
    await page.getByTestId('nav-workflow-templates').click()
    await expect(page).toHaveURL(/\/workflow-templates/, { timeout: 8000 })
    await expect(page.getByTestId('workflow-template-table')).toBeVisible({ timeout: 5000 })
  })

  test('should show 4 built-in templates', async ({ page }) => {
    await page.goto('/workflow-templates')
    await expect(page.getByTestId('workflow-template-table')).toBeVisible({ timeout: 8000 })

    // All 4 built-in templates should be visible
    await expect(page.getByTestId('template-row-STANDARD_DELIVERY')).toBeVisible({ timeout: 5000 })
    await expect(page.getByTestId('template-row-BACKEND_FOCUSED')).toBeVisible({ timeout: 5000 })
    await expect(page.getByTestId('template-row-FRONTEND_FOCUSED')).toBeVisible({ timeout: 5000 })
    await expect(page.getByTestId('template-row-REVIEW_ONLY')).toBeVisible({ timeout: 5000 })
  })

  test('should open template detail drawer', async ({ page }) => {
    await page.goto('/workflow-templates')
    await expect(page.getByTestId('workflow-template-table')).toBeVisible({ timeout: 8000 })

    await page.getByTestId('btn-view-template-STANDARD_DELIVERY').click()

    // Drawer should open
    await expect(page.getByTestId('template-detail-drawer')).toBeVisible({ timeout: 5000 })

    // Should show phase detail sections
    await expect(page.getByTestId('detail-phase-PLANNING')).toBeVisible({ timeout: 3000 })
    await expect(page.getByTestId('detail-phase-IMPLEMENTATION')).toBeVisible({ timeout: 3000 })
  })

  test('should disable BACKEND_FOCUSED and hide from strategy dropdown', async ({ page }) => {
    // Go to workflow templates
    await page.goto('/workflow-templates')
    await expect(page.getByTestId('workflow-template-table')).toBeVisible({ timeout: 8000 })

    // Disable BACKEND_FOCUSED
    await page.getByTestId('btn-toggle-template-BACKEND_FOCUSED').click()

    // Wait for status to update
    await expect(page.getByTestId('template-row-BACKEND_FOCUSED')).toContainText('停用', { timeout: 5000 })

    // Now go to multi-agent tab in a task
    const { projectId, taskId } = await createProjectAndTask(page)
    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    // Open strategy dropdown
    const strategySelect = page.getByTestId('strategy-select')
    await strategySelect.click()
    await page.waitForSelector('[data-testid="strategy-option-STANDARD_DELIVERY"]', { state: 'attached', timeout: 5000 })

    // BACKEND_FOCUSED should NOT be in the dropdown
    const backendOption = page.locator('[data-testid="strategy-option-BACKEND_FOCUSED"]')
    await expect(backendOption).not.toBeVisible({ timeout: 3000 })

    // STANDARD_DELIVERY should still be there
    await expect(page.locator('[data-testid="strategy-option-STANDARD_DELIVERY"]')).toBeAttached()
    // BACKEND_FOCUSED stays DISABLED — next test re-enables it
  })

  test('should re-enable BACKEND_FOCUSED and show in strategy dropdown', async ({ page }) => {
    // BACKEND_FOCUSED is disabled from previous test — re-enable it
    await page.goto('/workflow-templates')
    await expect(page.getByTestId('workflow-template-table')).toBeVisible({ timeout: 8000 })

    // Click toggle to re-enable (button shows 启用 when disabled)
    await page.getByTestId('btn-toggle-template-BACKEND_FOCUSED').click()
    // After re-enable, button text flips to 停用
    await expect(page.getByTestId('btn-toggle-template-BACKEND_FOCUSED')).toContainText('停用', { timeout: 5000 })

    // Verify it appears in multi-agent strategy dropdown
    const { projectId, taskId } = await createProjectAndTask(page)
    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    const strategySelect = page.getByTestId('strategy-select')
    await strategySelect.click()
    await page.waitForSelector('[data-testid="strategy-option-BACKEND_FOCUSED"]', { state: 'attached', timeout: 5000 })
  })

  test('should filter templates by status', async ({ page }) => {
    await page.goto('/workflow-templates')
    await expect(page.getByTestId('workflow-template-table')).toBeVisible({ timeout: 8000 })

    // Click "启用" filter - should show all 4
    await page.locator('.wftp-filter-chip').filter({ hasText: '启用' }).click()
    await expect(page.getByTestId('template-row-STANDARD_DELIVERY')).toBeVisible({ timeout: 3000 })
    await expect(page.getByTestId('template-row-BACKEND_FOCUSED')).toBeVisible({ timeout: 3000 })
    await expect(page.getByTestId('template-row-FRONTEND_FOCUSED')).toBeVisible({ timeout: 3000 })
    await expect(page.getByTestId('template-row-REVIEW_ONLY')).toBeVisible({ timeout: 3000 })

    // Click "停用" filter - should show none (all are enabled)
    await page.locator('.wftp-filter-chip').filter({ hasText: '停用' }).click()
    await expect(page.getByTestId('template-row-STANDARD_DELIVERY')).not.toBeVisible({ timeout: 3000 })
  })

  test('should not have JS errors on workflow template page', async ({ page }) => {
    const jsErrors: string[] = []
    page.on('pageerror', (err) => {
      jsErrors.push(err.message)
    })

    await page.goto('/workflow-templates')
    await expect(page.getByTestId('workflow-template-table')).toBeVisible({ timeout: 8000 })

    // Open detail drawer
    await page.getByTestId('btn-view-template-STANDARD_DELIVERY').click()
    await expect(page.getByTestId('template-detail-drawer')).toBeVisible({ timeout: 5000 })

    expect(jsErrors).toEqual([])
  })
})
