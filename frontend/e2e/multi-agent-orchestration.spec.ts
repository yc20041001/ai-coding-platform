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

async function enableMultiAgentAgents(page: ReturnType<typeof test['info']>['page'], projectId: string) {
  const token = await page.evaluate(() => localStorage.getItem('aicp_token'))
  const agentIds = [300001, 300002, 300003, 300004, 300005]
  for (const agentId of agentIds) {
    await page.evaluate(
      async ({ pid, aid, tok }) => {
        const res = await fetch(`/api/projects/${pid}/agents/${aid}/enable`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${tok}`,
          },
          body: '{}',
        })
        if (!res.ok) {
          throw new Error(`Enable agent failed: ${res.status}`)
        }
      },
      { pid: projectId, aid: agentId, tok: token },
    )
  }
}

async function createProjectAndTask(page: ReturnType<typeof test['info']>['page']): Promise<{ projectId: string; taskId: string }> {
  const suffix = Date.now().toString() + Math.random().toString(36).slice(2, 6)
  const projectName = `E2E-MultiAgent-${suffix}`
  const taskTitle = `E2E-MA-Task-${suffix}`

  // Create project
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

  // Navigate to project detail
  await page.getByTestId('project-table-area').getByText(projectName).first().click()
  await expect(page).toHaveURL(/\/projects\/\d+/, { timeout: 8000 })

  const projectId = page.url().split('/').pop()!

  // Enable multi-agent agents for this project
  await enableMultiAgentAgents(page, projectId)

  // Navigate to tasks
  await page.goto(`/projects/${projectId}/tasks`)
  await expect(page).toHaveURL(/\/projects\/\d+\/tasks/)

  // Create task
  await page.getByTestId('btn-create-task').click()
  const taskForm = page.getByTestId('dialog-create-task')
  await expect(taskForm).toBeVisible({ timeout: 5000 })

  await page.getByTestId('input-task-title').fill(taskTitle)
  await page.getByTestId('btn-submit-task').click()
  await expect(taskForm).not.toBeVisible({ timeout: 15000 })

  // Navigate to task detail by clicking the detail button
  const taskTable = page.getByTestId('task-table')
  await expect(taskTable).toContainText(taskTitle, { timeout: 8000 })
  await taskTable.getByTestId('btn-task-detail').first().click()

  await expect(page).toHaveURL(/\/projects\/\d+\/tasks\/\d+/, { timeout: 8000 })
  const taskId = page.url().split('/').pop()!

  return { projectId, taskId }
}

/**
 * Select a strategy from the el-select dropdown.
 * Element Plus teleports dropdown options to body, so Playwright visibility checks
 * are unreliable. We use evaluate() to dispatch clicks directly on DOM elements.
 */
async function selectStrategy(page: ReturnType<typeof test['info']>['page'], strategyKey: string) {
  const strategySelect = page.getByTestId('strategy-select')
  await strategySelect.click()
  // Wait for the option to render (Element Plus teleports to body)
  await page.waitForSelector(`[data-testid="strategy-option-${strategyKey}"]`, { state: 'attached', timeout: 5000 })
  await page.getByTestId(`strategy-option-${strategyKey}`).evaluate(el => (el as HTMLElement).click())
}

test.describe('Multi-Agent Orchestration', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should display multi-agent tab in task detail', async ({ page }) => {
    await createProjectAndTask(page)

    // Multi-agent tab should be visible in rail
    await expect(page.getByText('多智能体')).toBeVisible({ timeout: 8000 })
  })

  test('should start multi-agent run and show phase view', async ({ page }) => {
    await createProjectAndTask(page)

    // Click multi-agent tab
    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    // Click start button
    const startBtn = page.getByTestId('btn-start-multi-agent')
    await expect(startBtn).toBeVisible({ timeout: 5000 })
    await startBtn.click()

    // Wait for the run detail to appear
    await expect(page.getByTestId('multi-agent-run-detail')).toBeVisible({ timeout: 30000 })

    // Phase view should be visible
    await expect(page.getByTestId('multi-agent-phases')).toBeVisible({ timeout: 5000 })

    // Should have 4 phases
    const phaseCards = page.locator('.mar-phase-card')
    const phaseCount = await phaseCards.count()
    expect(phaseCount).toBe(4)

    // Summary stats should be visible
    await expect(page.getByTestId('multi-agent-summary-stats')).toBeVisible({ timeout: 3000 })

    // Approval gate card should be visible (paused at gate)
    await expect(page.getByTestId('multi-agent-approval-gate')).toBeVisible({ timeout: 5000 })
  })

  test('should show Phase 2 with three parallel lanes', async ({ page }) => {
    await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-run-detail')).toBeVisible({ timeout: 30000 })

    // Phase 2 (IMPLEMENTATION) should exist
    const phase2 = page.getByTestId('multi-agent-phase-IMPLEMENTATION')
    await expect(phase2).toBeVisible({ timeout: 5000 })

    // Should have 3 lanes: backend, frontend, test
    await expect(phase2.getByTestId('multi-agent-lane-backend')).toBeVisible({ timeout: 3000 })
    await expect(phase2.getByTestId('multi-agent-lane-frontend')).toBeVisible({ timeout: 3000 })
    await expect(phase2.getByTestId('multi-agent-lane-test')).toBeVisible({ timeout: 3000 })
  })

  test('should expand lane to show output and input context', async ({ page }) => {
    await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-run-detail')).toBeVisible({ timeout: 30000 })

    // Click first lane in Phase 1 to expand
    const phase1 = page.getByTestId('multi-agent-phase-PLANNING')
    await expect(phase1).toBeVisible({ timeout: 5000 })
    const architectLane = phase1.getByTestId('multi-agent-lane-architect')
    await expect(architectLane).toBeVisible({ timeout: 3000 })

    // Click lane header to expand
    await architectLane.locator('.mar-lane-header').click()

    // Should show output content
    await expect(architectLane.locator('.mar-step-output')).toBeVisible({ timeout: 3000 })

    // Click input context toggle
    await architectLane.getByTestId('multi-agent-step-input-toggle').click()

    // Input context should be visible
    await expect(architectLane.getByTestId('multi-agent-step-input')).toBeVisible({ timeout: 3000 })
  })

  test('should show message flow after run', async ({ page }) => {
    await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-run-detail')).toBeVisible({ timeout: 30000 })

    // Message flow view should be visible
    await expect(page.getByTestId('multi-agent-message-flow')).toBeVisible({ timeout: 5000 })

    // Should contain message type tags
    await expect(page.getByText('任务上下文')).toBeVisible({ timeout: 3000 })
    await expect(page.getByText('交接消息')).toBeVisible({ timeout: 3000 })
  })

  test('should show summary artifact in artifacts tab', async ({ page }) => {
    await createProjectAndTask(page)

    // Navigate to multi-agent tab and start run
    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })
    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-approval-gate')).toBeVisible({ timeout: 30000 })

    // Approve the gate so run completes
    await page.getByTestId('btn-approve-gate').click()
    await expect(page.getByTestId('multi-agent-final-summary')).toBeVisible({ timeout: 30000 })

    // Navigate to artifacts tab via rail button
    await page.locator('.sr-item').filter({ hasText: '产物' }).click()
    await expect(page.getByText('Multi-Agent Mock Orchestration Summary')).toBeVisible({ timeout: 8000 })
  })

  test('should not have JS errors on multi-agent page', async ({ page }) => {
    const jsErrors: string[] = []
    page.on('pageerror', (err) => {
      jsErrors.push(err.message)
    })

    await createProjectAndTask(page)

    // Go to multi-agent tab
    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    // Start run
    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-run-detail')).toBeVisible({ timeout: 30000 })

    // Phase view should show without errors
    await expect(page.getByTestId('multi-agent-phases')).toBeVisible({ timeout: 5000 })

    expect(jsErrors).toEqual([])
  })

  test('should show review lane with Phase 2 aggregation context', async ({ page }) => {
    await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-approval-gate')).toBeVisible({ timeout: 30000 })

    // Approve the gate so remaining phases execute
    await page.getByTestId('btn-approve-gate').click()
    await expect(page.getByTestId('multi-agent-final-summary')).toBeVisible({ timeout: 30000 })

    // Find Phase 3 (REVIEW) and expand the review lane
    const phase3 = page.getByTestId('multi-agent-phase-REVIEW')
    await expect(phase3).toBeVisible({ timeout: 5000 })
    const reviewLane = phase3.getByTestId('multi-agent-lane-review')
    await expect(reviewLane).toBeVisible({ timeout: 3000 })

    // Expand the lane
    await reviewLane.locator('.mar-lane-header').click()

    // Click input context toggle
    await reviewLane.getByTestId('multi-agent-step-input-toggle').click()

    // Input context should reference Phase 2
    const inputCtx = reviewLane.getByTestId('multi-agent-step-input')
    await expect(inputCtx).toBeVisible({ timeout: 3000 })
    await expect(inputCtx).toContainText('Phase 2', { timeout: 3000 })
  })

  test('should show final summary with phase and message counts', async ({ page }) => {
    await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-approval-gate')).toBeVisible({ timeout: 30000 })

    // Summary stats should be visible even while waiting approval
    const summaryStats = page.getByTestId('multi-agent-summary-stats')
    await expect(summaryStats).toBeVisible({ timeout: 3000 })

    // Should show Phases count
    await expect(summaryStats).toContainText('Phases', { timeout: 3000 })
    // Should show Message count
    await expect(summaryStats).toContainText('消息', { timeout: 3000 })

    // Approve the gate so run completes and final summary appears
    await page.getByTestId('btn-approve-gate').click()
    await expect(page.getByTestId('multi-agent-final-summary')).toBeVisible({ timeout: 30000 })

    // Final summary content should mention phases
    const finalSummary = page.getByTestId('multi-agent-final-summary')
    await expect(finalSummary.locator('.mar-summary-content')).toContainText('Phase', { timeout: 3000 })
  })

  // ========================
  // Strategy selection tests (35D)
  // ========================

  test('should show strategy dropdown with 4 options', async ({ page }) => {
    await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    // Strategy select should be visible
    const strategySelect = page.getByTestId('strategy-select')
    await expect(strategySelect).toBeVisible({ timeout: 5000 })

    // Click to open dropdown — Element Plus teleports dropdown to body
    await strategySelect.click()
    // Wait for options to render (attached to DOM, not necessarily visible)
    await page.waitForSelector('[data-testid="strategy-option-STANDARD_DELIVERY"]', { state: 'attached', timeout: 5000 })

    // Should have 4 strategy options in the dropdown
    const options = page.locator('.el-select-dropdown__item')
    const optionCount = await options.count()
    expect(optionCount).toBe(4)
  })

  test('should show BACKEND_FOCUSED template preview', async ({ page }) => {
    await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    // Select BACKEND_FOCUSED strategy
    await selectStrategy(page, 'BACKEND_FOCUSED')

    // Strategy preview should be visible
    const preview = page.getByTestId('strategy-preview')
    await expect(preview).toBeVisible({ timeout: 3000 })

    // Should show BACKEND_IMPLEMENTATION phase (no FRONTEND)
    await expect(page.getByTestId('strategy-preview-phase-BACKEND_IMPLEMENTATION')).toBeVisible({ timeout: 3000 })

    // Should NOT have FRONTEND_IMPLEMENTATION phase
    const frontendPhase = page.getByTestId('strategy-preview-phase-FRONTEND_IMPLEMENTATION')
    await expect(frontendPhase).not.toBeVisible()
  })

  test('should BACKEND_FOCUSED run skip frontend lane', async ({ page }) => {
    await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    // Select BACKEND_FOCUSED strategy and start
    await selectStrategy(page, 'BACKEND_FOCUSED')

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-run-detail')).toBeVisible({ timeout: 30000 })

    // BACKEND_IMPLEMENTATION phase should exist
    await expect(page.getByTestId('multi-agent-phase-BACKEND_IMPLEMENTATION')).toBeVisible({ timeout: 5000 })

    // Should have backend and test lanes
    const backendPhase = page.getByTestId('multi-agent-phase-BACKEND_IMPLEMENTATION')
    await expect(backendPhase.getByTestId('multi-agent-lane-backend')).toBeVisible({ timeout: 3000 })
    await expect(backendPhase.getByTestId('multi-agent-lane-test')).toBeVisible({ timeout: 3000 })

    // Should NOT have frontend lane anywhere
    const frontendLanes = page.getByTestId('multi-agent-lane-frontend')
    await expect(frontendLanes).not.toBeVisible()
  })

  test('should REVIEW_ONLY run have 2 phases', async ({ page }) => {
    await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    // Select REVIEW_ONLY strategy and start
    await selectStrategy(page, 'REVIEW_ONLY')

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-run-detail')).toBeVisible({ timeout: 30000 })

    // Should only have 2 phase cards
    const phaseCards = page.locator('.mar-phase-card')
    const phaseCount = await phaseCards.count()
    expect(phaseCount).toBe(2)

    // Should have REVIEW and SUMMARY phases
    await expect(page.getByTestId('multi-agent-phase-REVIEW')).toBeVisible({ timeout: 5000 })
    await expect(page.getByTestId('multi-agent-phase-SUMMARY')).toBeVisible({ timeout: 5000 })
  })

  test('should display strategy name on run detail', async ({ page }) => {
    await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    // Select BACKEND_FOCUSED
    await selectStrategy(page, 'BACKEND_FOCUSED')

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-run-detail')).toBeVisible({ timeout: 30000 })

    // Run meta should display strategy name (Chinese name)
    const runMeta = page.locator('.mar-run-meta')
    await expect(runMeta).toContainText('后端优先流程', { timeout: 3000 })
  })

  test('should not have JS errors when switching strategies', async ({ page }) => {
    const jsErrors: string[] = []
    page.on('pageerror', (err) => {
      jsErrors.push(err.message)
    })

    await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    // Switch through all strategies
    await selectStrategy(page, 'BACKEND_FOCUSED')
    await expect(page.getByTestId('strategy-preview')).toBeVisible({ timeout: 3000 })

    await selectStrategy(page, 'FRONTEND_FOCUSED')
    await expect(page.getByTestId('strategy-preview')).toBeVisible({ timeout: 3000 })

    await selectStrategy(page, 'REVIEW_ONLY')
    await expect(page.getByTestId('strategy-preview')).toBeVisible({ timeout: 3000 })

    await selectStrategy(page, 'STANDARD_DELIVERY')
    await expect(page.getByTestId('strategy-preview')).toBeVisible({ timeout: 3000 })

    // Start a run with STANDARD_DELIVERY
    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-run-detail')).toBeVisible({ timeout: 30000 })

    expect(jsErrors).toEqual([])
  })

  test('should show approval gate card on STANDARD_DELIVERY run', async ({ page }) => {
    await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    // Select STANDARD_DELIVERY and start
    await selectStrategy(page, 'STANDARD_DELIVERY')

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-run-detail')).toBeVisible({ timeout: 30000 })

    // Approval gate card should be visible
    await expect(page.getByTestId('multi-agent-approval-gate')).toBeVisible({ timeout: 5000 })

    // Approve button and reject button should be visible
    await expect(page.getByTestId('btn-approve-gate')).toBeVisible()
    await expect(page.getByTestId('btn-reject-gate')).toBeVisible()

    // Approval comment input should be visible
    await expect(page.getByTestId('approval-comment-input')).toBeVisible()
  })

  test('should approve gate and complete run', async ({ page }) => {
    await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    // Select STANDARD_DELIVERY and start
    await selectStrategy(page, 'STANDARD_DELIVERY')

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-approval-gate')).toBeVisible({ timeout: 30000 })

    // Enter approval comment
    await page.getByTestId('approval-comment-input').fill('E2E审批通过')
    await page.getByTestId('btn-approve-gate').click()

    // Wait for run to complete
    await expect(page.getByTestId('multi-agent-final-summary')).toBeVisible({ timeout: 30000 })

    // Run status should show COMPLETED
    const runMeta = page.locator('.mar-run-meta')
    await expect(runMeta).toContainText('COMPLETED', { timeout: 5000 })
  })

  test('should reject gate and cancel run', async ({ page }) => {
    await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    // Select STANDARD_DELIVERY and start
    await selectStrategy(page, 'STANDARD_DELIVERY')

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-approval-gate')).toBeVisible({ timeout: 30000 })

    // Enter rejection comment
    await page.getByTestId('approval-comment-input').fill('E2E驳回意见')
    await page.getByTestId('btn-reject-gate').click()

    // Run status should show CANCELED
    const runMeta = page.locator('.mar-run-meta')
    await expect(runMeta).toContainText('CANCELED', { timeout: 5000 })
  })

  test('should REVIEW_ONLY strategy complete without approval card', async ({ page }) => {
    await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    // Select REVIEW_ONLY and start
    await selectStrategy(page, 'REVIEW_ONLY')

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-run-detail')).toBeVisible({ timeout: 30000 })

    // No approval card should appear
    await expect(page.getByTestId('multi-agent-approval-gate')).not.toBeVisible({ timeout: 3000 })

    // Should complete directly
    await expect(page.getByTestId('multi-agent-final-summary')).toBeVisible({ timeout: 30000 })

    const runMeta = page.locator('.mar-run-meta')
    await expect(runMeta).toContainText('COMPLETED', { timeout: 5000 })
  })

  test('should not have JS errors during approval gate flow', async ({ page }) => {
    const jsErrors: string[] = []
    page.on('pageerror', (err) => {
      jsErrors.push(err.message)
    })

    await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    // Select STANDARD_DELIVERY and start
    await selectStrategy(page, 'STANDARD_DELIVERY')

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-approval-gate')).toBeVisible({ timeout: 30000 })

    // Approve
    await page.getByTestId('btn-approve-gate').click()
    await expect(page.getByTestId('multi-agent-final-summary')).toBeVisible({ timeout: 30000 })

    // Verify approval record shows in history
    await expect(page.getByTestId('multi-agent-approval-gates-history')).toBeVisible({ timeout: 5000 })
    await expect(page.getByTestId('approval-gate-record-APPROVED')).toBeVisible({ timeout: 3000 })

    expect(jsErrors).toEqual([])
  })
})
