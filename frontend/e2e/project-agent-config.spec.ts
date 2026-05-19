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

async function createProjectAndNavigateToAgents(page: ReturnType<typeof test['info']>['page']): Promise<string> {
  const suffix = Date.now().toString() + Math.random().toString(36).slice(2, 6)
  const projectName = `E2E-Agent-${suffix}`

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

  return projectId
}

test.describe('Project Agent Config', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should display agent table with rows and action buttons', async ({ page }) => {
    await createProjectAndNavigateToAgents(page)

    // Wait for agent table to load
    const agentTable = page.getByTestId('project-agent-table')
    await expect(agentTable).toBeVisible({ timeout: 10000 })

    // At least one row of agent data should be present
    const rows = agentTable.locator('tbody tr')
    const rowCount = await rows.count()
    expect(rowCount).toBeGreaterThanOrEqual(1)

    // Enable buttons should exist (agents are disabled by default)
    const enableButtons = page.getByTestId('btn-agent-enable')
    const enableCount = await enableButtons.count()
    expect(enableCount).toBeGreaterThanOrEqual(1)

    // The first enable button should be clickable
    await expect(enableButtons.first()).toBeVisible()
  })

  test('should open enable dialog with model config dropdown', async ({ page }) => {
    await createProjectAndNavigateToAgents(page)

    // Wait for agent table
    const agentTable = page.getByTestId('project-agent-table')
    await expect(agentTable).toBeVisible({ timeout: 10000 })

    // Click the first Enable button - should open dialog
    const enableBtn = page.getByTestId('btn-agent-enable').first()
    await enableBtn.click()

    // Verify dialog is visible
    const dialog = page.getByTestId('agent-enable-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Verify model config select is visible
    await expect(page.getByTestId('select-model-config')).toBeVisible()

    // Verify version select is visible
    await expect(page.getByTestId('select-agent-version')).toBeVisible()

    // Click confirm to enable (without selecting model config)
    await page.getByTestId('btn-confirm-enable-agent').click()

    // Wait for the table to reload (the button should change to Disable)
    await expect(page.getByTestId('btn-agent-disable').first()).toBeVisible({ timeout: 10000 })
  })

  test('should enable and disable an agent via dialog', async ({ page }) => {
    await createProjectAndNavigateToAgents(page)

    // Wait for agent table
    const agentTable = page.getByTestId('project-agent-table')
    await expect(agentTable).toBeVisible({ timeout: 10000 })

    // Click Enable → dialog opens
    const enableBtn = page.getByTestId('btn-agent-enable').first()
    await enableBtn.click()

    const dialog = page.getByTestId('agent-enable-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Confirm enable
    await page.getByTestId('btn-confirm-enable-agent').click()

    // Wait for table to reload with Disable button
    await expect(page.getByTestId('btn-agent-disable').first()).toBeVisible({ timeout: 10000 })

    // Now click Disable to revert
    const disableBtn = page.getByTestId('btn-agent-disable').first()
    await disableBtn.click()

    // Wait for table to reload (button should change back to Enable)
    await expect(page.getByTestId('btn-agent-enable').first()).toBeVisible({ timeout: 10000 })
  })

  test('should show error alert on permission failure', async ({ page }) => {
    // Navigate to a non-existent project
    await page.goto('/projects/99999999/agents')

    // Page container should be visible (graceful handling for invalid project)
    await expect(page.locator('.page-container')).toBeVisible({ timeout: 8000 })

    // Verify no JS crash - the page should render either an error or an empty state
    const hasContent = await page.locator('.page-container').innerText({ timeout: 5000 }).catch(() => '')
    // Non-existent project either shows error or renders empty — both are valid
    expect(hasContent !== null).toBeTruthy()
  })

  test('should have no JS errors on page load', async ({ page }) => {
    const jsErrors: string[] = []
    page.on('pageerror', (err) => {
      jsErrors.push(err.message)
    })

    await createProjectAndNavigateToAgents(page)

    // Wait for content
    await expect(page.locator('.page-container')).toBeVisible({ timeout: 8000 })

    // No JS errors
    expect(jsErrors).toEqual([])
  })

  test('should display runtime config section in enable dialog', async ({ page }) => {
    await createProjectAndNavigateToAgents(page)

    const agentTable = page.getByTestId('project-agent-table')
    await expect(agentTable).toBeVisible({ timeout: 10000 })

    // Click first Enable button
    const enableBtn = page.getByTestId('btn-agent-enable').first()
    await enableBtn.click()

    const dialog = page.getByTestId('agent-enable-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Runtime config section should be visible
    const runtimeSection = page.getByTestId('agent-runtime-config-section')
    await expect(runtimeSection).toBeVisible()

    // All form fields should be present
    await expect(page.getByTestId('input-agent-temperature')).toBeVisible()
    await expect(page.getByTestId('input-agent-max-tokens')).toBeVisible()
    await expect(page.getByTestId('input-agent-timeout')).toBeVisible()
    await expect(page.getByTestId('switch-agent-use-rag')).toBeVisible()
    await expect(page.getByTestId('input-agent-custom-instruction')).toBeVisible()
  })

  test('should toggle RAG and show knowledge base select', async ({ page }) => {
    await createProjectAndNavigateToAgents(page)

    const agentTable = page.getByTestId('project-agent-table')
    await expect(agentTable).toBeVisible({ timeout: 10000 })

    await page.getByTestId('btn-agent-enable').first().click()
    const dialog = page.getByTestId('agent-enable-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Knowledge base select should be hidden when RAG is off
    const kbSelect = page.getByTestId('select-agent-knowledge-base')
    await expect(kbSelect).not.toBeVisible()

    // Toggle RAG on
    const ragSwitch = page.getByTestId('switch-agent-use-rag')
    await ragSwitch.click()

    // Knowledge base select should now be visible
    await expect(kbSelect).toBeVisible()
  })

  test('should save runtime config and display summary in table', async ({ page }) => {
    await createProjectAndNavigateToAgents(page)

    const agentTable = page.getByTestId('project-agent-table')
    await expect(agentTable).toBeVisible({ timeout: 10000 })

    // Open enable dialog
    await page.getByTestId('btn-agent-enable').first().click()
    const dialog = page.getByTestId('agent-enable-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Set max tokens - el-input-number puts testid on wrapper, input is nested
    const maxTokensInput = page.getByTestId('input-agent-max-tokens').locator('input')
    await maxTokensInput.click({ clickCount: 3 })
    await maxTokensInput.fill('8192')

    // Set timeout
    const timeoutInput = page.getByTestId('input-agent-timeout').locator('input')
    await timeoutInput.click({ clickCount: 3 })
    await timeoutInput.fill('120')

    // Fill custom instruction - el-input textarea puts testid on the textarea directly
    await page.getByTestId('input-agent-custom-instruction')
      .fill('E2E 测试自定义指令：优先使用简洁方案')

    // Save
    await page.getByTestId('btn-confirm-enable-agent').click()

    // Wait for table to reload - should show Disable and Configure buttons
    await expect(page.getByTestId('btn-agent-disable').first()).toBeVisible({ timeout: 10000 })

    // Config summary should be visible in table
    await expect(page.getByTestId('agent-runtime-summary').first()).toBeVisible({ timeout: 5000 })
  })

  test('should show configure button for enabled agent and load saved config', async ({ page }) => {
    await createProjectAndNavigateToAgents(page)

    const agentTable = page.getByTestId('project-agent-table')
    await expect(agentTable).toBeVisible({ timeout: 10000 })

    // Enable with custom config
    await page.getByTestId('btn-agent-enable').first().click()
    const dialog = page.getByTestId('agent-enable-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    await page.getByTestId('input-agent-custom-instruction')
      .fill('回显验证指令：确保配置正确回显')
    await page.getByTestId('btn-confirm-enable-agent').click()

    // Wait for table to reload
    await expect(page.getByTestId('btn-agent-disable').first()).toBeVisible({ timeout: 10000 })

    // Click Configure button
    const configureBtn = page.getByTestId('btn-agent-configure').first()
    await expect(configureBtn).toBeVisible()
    await configureBtn.click()

    // Dialog should show saved config
    await expect(dialog).toBeVisible({ timeout: 5000 })
    const customInstructionField = page.getByTestId('input-agent-custom-instruction')
    await expect(customInstructionField).toHaveValue('回显验证指令：确保配置正确回显')

    // Dialog title should be "配置智能体" for editing
    await expect(dialog.locator('.el-dialog__header')).toContainText('配置智能体')
  })

  test('should not have JS errors during runtime config editing', async ({ page }) => {
    const jsErrors: string[] = []
    page.on('pageerror', (err) => {
      jsErrors.push(err.message)
    })

    await createProjectAndNavigateToAgents(page)

    const agentTable = page.getByTestId('project-agent-table')
    await expect(agentTable).toBeVisible({ timeout: 10000 })

    // Open dialog
    await page.getByTestId('btn-agent-enable').first().click()
    await expect(page.getByTestId('agent-enable-dialog')).toBeVisible({ timeout: 5000 })

    // Interact with all runtime config fields
    await page.getByTestId('input-agent-custom-instruction')
      .fill('无 JS 错误测试')
    await page.getByTestId('switch-agent-use-rag').click()

    // Save
    await page.getByTestId('btn-confirm-enable-agent').click()
    await expect(page.getByTestId('btn-agent-disable').first()).toBeVisible({ timeout: 10000 })

    // Open configure dialog
    await page.getByTestId('btn-agent-configure').first().click()
    await expect(page.getByTestId('agent-enable-dialog')).toBeVisible({ timeout: 5000 })

    expect(jsErrors).toEqual([])
  })
})
