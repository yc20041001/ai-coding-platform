import { test, expect } from '@playwright/test'

const ADMIN_EMAIL = 'admin@example.com'
const ADMIN_PASSWORD = 'Admin@123456'
const SUFFIX = Date.now().toString()
const PROJECT_NAME = `E2E-Proj-${SUFFIX}`
const TASK_TITLE = `E2E-Task-${SUFFIX}`
const CHAT_TITLE = `E2E-Chat-${SUFFIX}`

async function login(page: ReturnType<typeof test['info']>['page']) {
  await page.goto('/login')
  await page.getByTestId('login-email').fill(ADMIN_EMAIL)
  await page.getByTestId('login-password').fill(ADMIN_PASSWORD)
  await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
}

test.describe('Project, Task & Chat', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should create a project', async ({ page }) => {
    // Navigate to projects list
    await page.goto('/projects')
    await expect(page).toHaveURL(/\/projects/)
    await expect(page.locator('.page-container')).toBeVisible({ timeout: 8000 })

    // Click create button and wait for dialog form
    await page.getByTestId('btn-create-project').click()
    const dialogForm = page.getByTestId('dialog-create-project')
    await expect(dialogForm).toBeVisible({ timeout: 5000 })

    // Fill project name
    await page.getByTestId('input-project-name').fill(PROJECT_NAME)

    // Submit and wait for API response
    await Promise.all([
      page.waitForResponse(
        resp => resp.url().includes('/api/projects') && resp.request().method() === 'POST' && resp.status() === 200,
        { timeout: 15000 },
      ),
      page.getByTestId('btn-submit-project').click(),
    ])

    // Dialog should close
    await expect(dialogForm).not.toBeVisible({ timeout: 8000 })

    // New project should appear in the table
    await expect(page.getByTestId('project-table-area')).toContainText(PROJECT_NAME, { timeout: 8000 })
  })

  test('should create and execute a task', async ({ page }) => {
    // Navigate to projects list
    await page.goto('/projects')
    await expect(page).toHaveURL(/\/projects/)

    // Create a project first
    await page.getByTestId('btn-create-project').click()
    const projectForm = page.getByTestId('dialog-create-project')
    await expect(projectForm).toBeVisible({ timeout: 5000 })

    await page.getByTestId('input-project-name').fill(PROJECT_NAME)
    await Promise.all([
      page.waitForResponse(
        resp => resp.url().includes('/api/projects') && resp.request().method() === 'POST' && resp.status() === 200,
        { timeout: 15000 },
      ),
      page.getByTestId('btn-submit-project').click(),
    ])
    await expect(projectForm).not.toBeVisible({ timeout: 8000 })

    // Click on the new project row to open detail (use .first() since both tests create same-named project)
    await page.getByTestId('project-table-area').getByText(PROJECT_NAME).first().click()
    await expect(page).toHaveURL(/\/projects\/\d+/, { timeout: 8000 })

    // Navigate to Tasks tab via URL
    const projectId = page.url().split('/').pop()
    await page.goto(`/projects/${projectId}/tasks`)
    await expect(page).toHaveURL(/\/projects\/\d+\/tasks/)

    // Click create task button
    await page.getByTestId('btn-create-task').click()
    const taskForm = page.getByTestId('dialog-create-task')
    await expect(taskForm).toBeVisible({ timeout: 5000 })

    // Fill task form and submit
    await page.getByTestId('input-task-title').fill(TASK_TITLE)
    await page.getByTestId('btn-submit-task').click()

    // Wait for dialog to close (indicates API success)
    await expect(taskForm).not.toBeVisible({ timeout: 15000 })

    // Task should appear in table
    const taskTable = page.getByTestId('task-table')
    await expect(taskTable).toContainText(TASK_TITLE, { timeout: 8000 })

    // Click Execute on the new task
    const taskRow = taskTable.locator('tr').filter({ hasText: TASK_TITLE })
    await taskRow.getByTestId('btn-execute-task').click()

    const executeForm = page.getByTestId('dialog-execute-task')
    await expect(executeForm).toBeVisible({ timeout: 5000 })

    await Promise.all([
      page.waitForResponse(
        resp => resp.url().includes('/api/tasks/') && resp.url().includes('/execute') && resp.request().method() === 'POST' && resp.status() === 200,
        { timeout: 30000 },
      ),
      page.getByTestId('btn-submit-execute').click(),
    ])

    // Execution should succeed — task status should no longer be PENDING
    await expect(taskRow.getByText(/COMPLETED|RUNNING/)).toBeVisible({ timeout: 15000 })
  })

  test('should create a chat session and send a message', async ({ page }) => {
    // Navigate to projects list
    await page.goto('/projects')
    await expect(page).toHaveURL(/\/projects/)
    await expect(page.locator('.page-container')).toBeVisible({ timeout: 8000 })

    // Click on first project row (must have at least one)
    const tableArea = page.getByTestId('project-table-area')
    await expect(tableArea).toBeVisible({ timeout: 8000 })
    const tableRows = page.getByTestId('project-table')
    await expect(tableRows).toBeVisible({ timeout: 5000 })
    const firstRow = tableRows.locator('tr').nth(1)
    await firstRow.click()
    await expect(page).toHaveURL(/\/projects\/\d+/, { timeout: 8000 })

    // Navigate to Chat tab
    const projectId = page.url().split('/').pop()
    await page.goto(`/projects/${projectId}/chat`)
    await expect(page).toHaveURL(/\/projects\/\d+\/chat/)

    // Create a new chat session
    await expect(page.getByTestId('chat-session-list')).toBeVisible({ timeout: 8000 })

    // Fill session name and create
    const sessionInput = page.getByPlaceholder('新建会话...')
    await sessionInput.fill(CHAT_TITLE)
    await sessionInput.press('Enter')

    // Wait for session to appear in list
    await expect(page.getByTestId('chat-session-list')).toContainText(CHAT_TITLE, { timeout: 8000 })

    // Type and send a message
    await page.getByTestId('chat-message-input').fill('Hello, this is an E2E test message.')
    await expect(page.getByTestId('btn-send-message')).toBeEnabled()

    await page.getByTestId('btn-send-message').click()

    // Wait for streaming to start and complete
    await expect(page.getByTestId('chat-session-list')).toBeVisible({ timeout: 30000 })
    // Streaming indicator should appear then disappear
    await expect(page.locator('.chat-msg--streaming')).not.toBeVisible({ timeout: 30000 })
  })
})
