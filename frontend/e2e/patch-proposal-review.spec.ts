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

async function enableHighToolViaApi(page: ReturnType<typeof test['info']>['page'], projectId: string) {
  const token = await page.evaluate(() => localStorage.getItem('aicp_token'))
  await page.evaluate(
    async ({ pid, tok }) => {
      const res = await fetch(`/api/projects/${pid}/tools/910006/enable`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${tok}`,
        },
        body: '{}',
      })
      if (!res.ok) {
        throw new Error(`Enable HIGH tool failed: ${res.status}`)
      }
    },
    { pid: projectId, tok: token },
  )
}

async function createProjectAndTask(page: ReturnType<typeof test['info']>['page']): Promise<{ projectId: string; taskId: string }> {
  const suffix = Date.now().toString() + Math.random().toString(36).slice(2, 6)
  const projectName = `E2E-PPR-${suffix}`
  const taskTitle = `E2E-PPR-Task-${suffix}`

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

  // Enable HIGH tool
  await enableHighToolViaApi(page, projectId)

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

  // Navigate to task detail
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
  await page.waitForSelector(`[data-testid="strategy-option-${strategyKey}"]`, { state: 'attached', timeout: 5000 })
  await page.getByTestId(`strategy-option-${strategyKey}`).evaluate(el => (el as HTMLElement).click())
}

/**
 * Approve all WAITING_APPROVAL tools in the multi-agent run.
 * Returns true if at least one tool was approved.
 */
async function approveAllWaitingTools(page: ReturnType<typeof test['info']>['page']): Promise<boolean> {
  const phases = page.locator('[data-testid^="multi-agent-phase-"]')
  const phaseCount = await phases.count()
  let approved = false

  for (let i = 0; i < phaseCount; i++) {
    const phase = phases.nth(i)
    const lanes = phase.locator('[data-testid^="multi-agent-lane-"]')
    const laneCount = await lanes.count()
    for (let j = 0; j < laneCount; j++) {
      const lane = lanes.nth(j)
      await lane.locator('.mar-lane-header').click()
      const approveBtn = lane.locator('[data-testid="btn-approve-tool"]')
      if (await approveBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
        await approveBtn.click()
        await page.waitForTimeout(1000)
        approved = true
      }
    }
  }
  return approved
}

test.describe('Patch Proposal Review (36H)', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should display PatchProposalReviewPanel with safety banner and file list', async ({ page }) => {
    const { projectId } = await createProjectAndTask(page)

    // Navigate to multi-agent tab
    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    // Use REVIEW_ONLY to bypass approval gate
    await selectStrategy(page, 'REVIEW_ONLY')
    await page.waitForTimeout(300)

    // Start run
    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-run-detail')).toBeVisible({ timeout: 30000 })

    // Approve all WAITING_APPROVAL tools to trigger artifact creation
    const approved = await approveAllWaitingTools(page)
    expect(approved).toBeTruthy()

    // Navigate to artifacts tab via rail
    await page.locator('.sr-item').filter({ hasText: '产物' }).click()

    // Wait for artifact buttons to load
    const patchBtn = page.locator('.artifact-tabs button').filter({ hasText: /Mock Patch Proposal/ })
    await expect(patchBtn.first()).toBeVisible({ timeout: 10000 })
    await patchBtn.first().click()
    await page.waitForTimeout(500)

    // The PatchProposalReviewPanel should be visible
    await expect(page.getByTestId('patch-review-panel')).toBeVisible({ timeout: 5000 })

    // Safety banner should be visible
    await expect(page.getByTestId('patch-review-safety-banner')).toBeVisible({ timeout: 3000 })
    await expect(page.getByTestId('patch-review-safety-banner')).toContainText('安全提示', { timeout: 3000 })

    // File list should be visible
    await expect(page.getByTestId('patch-review-file-list')).toBeVisible({ timeout: 3000 })

    // Diff content should be visible
    await expect(page.getByTestId('patch-review-diff')).toBeVisible({ timeout: 3000 })
  })

  test('should display checklist with interactive checkboxes', async ({ page }) => {
    const { projectId } = await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    await selectStrategy(page, 'REVIEW_ONLY')
    await page.waitForTimeout(300)

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-run-detail')).toBeVisible({ timeout: 30000 })

    await approveAllWaitingTools(page)

    await page.locator('.sr-item').filter({ hasText: '产物' }).click()

    const patchBtn = page.locator('.artifact-tabs button').filter({ hasText: /Mock Patch Proposal/ })
    await expect(patchBtn.first()).toBeVisible({ timeout: 10000 })
    await patchBtn.first().click()
    await page.waitForTimeout(500)

    await expect(page.getByTestId('patch-review-panel')).toBeVisible({ timeout: 5000 })

    // Checklist section should be visible
    await expect(page.getByTestId('patch-review-checklist')).toBeVisible({ timeout: 3000 })

    // All checklist items should be present
    await expect(page.getByTestId('patch-review-check-matches')).toBeVisible({ timeout: 3000 })
    await expect(page.getByTestId('patch-review-check-sensitive')).toBeVisible({ timeout: 3000 })
    await expect(page.getByTestId('patch-review-check-filewrite')).toBeVisible({ timeout: 3000 })
    await expect(page.getByTestId('patch-review-check-gitop')).toBeVisible({ timeout: 3000 })
    await expect(page.getByTestId('patch-review-check-ready')).toBeVisible({ timeout: 3000 })

    // The readonly checkboxes (noFileWritten, noGitOperation) should be disabled and pre-checked
    const fileWriteCheckbox = page.getByTestId('patch-review-check-filewrite').locator('.el-checkbox')
    const gitOpCheckbox = page.getByTestId('patch-review-check-gitop').locator('.el-checkbox')

    // They should have the is-disabled class
    await expect(fileWriteCheckbox).toHaveClass(/is-disabled/, { timeout: 3000 })
    await expect(gitOpCheckbox).toHaveClass(/is-disabled/, { timeout: 3000 })

    // Toggle interactive checkboxes
    const matchesCheckbox = page.getByTestId('patch-review-check-matches').locator('.el-checkbox')
    const sensitiveCheckbox = page.getByTestId('patch-review-check-sensitive').locator('.el-checkbox')
    const readyCheckbox = page.getByTestId('patch-review-check-ready').locator('.el-checkbox')

    await matchesCheckbox.click()
    await sensitiveCheckbox.click()
    await readyCheckbox.click()

    // Check they are selected
    await expect(matchesCheckbox).toHaveClass(/is-checked/, { timeout: 3000 })
    await expect(sensitiveCheckbox).toHaveClass(/is-checked/, { timeout: 3000 })
    await expect(readyCheckbox).toHaveClass(/is-checked/, { timeout: 3000 })
  })

  test('should submit ACCEPTED_AS_PLAN decision and show REVIEWED status', async ({ page }) => {
    const { projectId } = await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    await selectStrategy(page, 'REVIEW_ONLY')
    await page.waitForTimeout(300)

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-run-detail')).toBeVisible({ timeout: 30000 })

    await approveAllWaitingTools(page)

    await page.locator('.sr-item').filter({ hasText: '产物' }).click()

    const patchBtn = page.locator('.artifact-tabs button').filter({ hasText: /Mock Patch Proposal/ })
    await expect(patchBtn.first()).toBeVisible({ timeout: 10000 })
    await patchBtn.first().click()
    await page.waitForTimeout(500)

    await expect(page.getByTestId('patch-review-panel')).toBeVisible({ timeout: 5000 })

    // Check all interactive checklist items
    await page.getByTestId('patch-review-check-matches').locator('.el-checkbox').click()
    await page.getByTestId('patch-review-check-sensitive').locator('.el-checkbox').click()
    await page.getByTestId('patch-review-check-ready').locator('.el-checkbox').click()

    // Confirm safety
    await page.getByTestId('patch-review-safety-confirmed').locator('.el-checkbox').click()

    // Enter a review comment
    await page.getByTestId('patch-review-comment').fill('E2E test: approved as plan')

    // Click accept as plan button
    await page.getByTestId('btn-accept-patch-plan').click()

    // Wait for the decision to be submitted and status to update
    await expect(page.getByTestId('patch-review-status')).toBeVisible({ timeout: 10000 })
    await expect(page.getByTestId('patch-review-decision-tag')).toBeVisible({ timeout: 5000 })

    // Decision tag should show ACCEPTED_AS_PLAN
    await expect(page.getByTestId('patch-review-decision-tag')).toContainText('接受为计划', { timeout: 3000 })

    // Decision buttons should be replaced by a "completed" message
    await expect(page.getByText('审阅已完成，不可重复提交。')).toBeVisible({ timeout: 3000 })
  })

  test('should submit REJECTED decision via review panel', async ({ page }) => {
    const { projectId } = await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    await selectStrategy(page, 'REVIEW_ONLY')
    await page.waitForTimeout(300)

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-run-detail')).toBeVisible({ timeout: 30000 })

    await approveAllWaitingTools(page)

    await page.locator('.sr-item').filter({ hasText: '产物' }).click()

    const patchBtn = page.locator('.artifact-tabs button').filter({ hasText: /Mock Patch Proposal/ })
    await expect(patchBtn.first()).toBeVisible({ timeout: 10000 })
    await patchBtn.first().click()
    await page.waitForTimeout(500)

    await expect(page.getByTestId('patch-review-panel')).toBeVisible({ timeout: 5000 })

    // Check all interactive checklist items
    await page.getByTestId('patch-review-check-matches').locator('.el-checkbox').click()
    await page.getByTestId('patch-review-check-sensitive').locator('.el-checkbox').click()
    await page.getByTestId('patch-review-check-ready').locator('.el-checkbox').click()

    // Confirm safety
    await page.getByTestId('patch-review-safety-confirmed').locator('.el-checkbox').click()

    // Click reject button
    await page.getByTestId('btn-reject-patch-proposal').click()

    // Wait for decision
    await expect(page.getByTestId('patch-review-status')).toBeVisible({ timeout: 10000 })
    await expect(page.getByTestId('patch-review-decision-tag')).toBeVisible({ timeout: 5000 })
    await expect(page.getByTestId('patch-review-decision-tag')).toContainText('已拒绝', { timeout: 3000 })
  })

  test('should require safety confirmation before submitting decision', async ({ page }) => {
    const { projectId } = await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    await selectStrategy(page, 'REVIEW_ONLY')
    await page.waitForTimeout(300)

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-run-detail')).toBeVisible({ timeout: 30000 })

    await approveAllWaitingTools(page)

    await page.locator('.sr-item').filter({ hasText: '产物' }).click()

    const patchBtn = page.locator('.artifact-tabs button').filter({ hasText: /Mock Patch Proposal/ })
    await expect(patchBtn.first()).toBeVisible({ timeout: 10000 })
    await patchBtn.first().click()
    await page.waitForTimeout(500)

    await expect(page.getByTestId('patch-review-panel')).toBeVisible({ timeout: 5000 })

    // Check interactive checkboxes but NOT safety confirmation
    await page.getByTestId('patch-review-check-matches').locator('.el-checkbox').click()
    await page.getByTestId('patch-review-check-sensitive').locator('.el-checkbox').click()
    await page.getByTestId('patch-review-check-ready').locator('.el-checkbox').click()

    // Accept button should be disabled because safetyConfirmed is false
    await expect(page.getByTestId('btn-accept-patch-plan')).toBeDisabled({ timeout: 3000 })
    await expect(page.getByTestId('btn-reject-patch-proposal')).toBeDisabled({ timeout: 3000 })
    await expect(page.getByTestId('btn-needs-patch-changes')).toBeDisabled({ timeout: 3000 })
    await expect(page.getByTestId('btn-mark-patch-reviewed')).toBeDisabled({ timeout: 3000 })

    // Now check safety confirmation
    await page.getByTestId('patch-review-safety-confirmed').locator('.el-checkbox').click()

    // Buttons should now be enabled
    await expect(page.getByTestId('btn-accept-patch-plan')).toBeEnabled({ timeout: 3000 })
  })

  test('should show patch review status in multi-agent run panel', async ({ page }) => {
    const { projectId } = await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    await selectStrategy(page, 'REVIEW_ONLY')
    await page.waitForTimeout(300)

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-run-detail')).toBeVisible({ timeout: 30000 })

    // Approve all tools
    const approved = await approveAllWaitingTools(page)
    expect(approved).toBeTruthy()

    // Wait for tool artifact link and review status to appear
    // The tool card should display a review status tag after artifact is created
    const reviewStatusTag = page.getByTestId('tool-patch-review-status').first()
    await expect(reviewStatusTag).toBeVisible({ timeout: 10000 })

    // The status should show "待审阅" (PENDING)
    await expect(reviewStatusTag).toContainText('待审阅', { timeout: 5000 })
  })

  test('should not have JS errors with patch review panel', async ({ page }) => {
    const jsErrors: string[] = []
    page.on('pageerror', (err) => {
      jsErrors.push(err.message)
    })

    const { projectId } = await createProjectAndTask(page)

    await page.getByText('多智能体').click()
    await expect(page.getByTestId('multi-agent-tab')).toBeVisible({ timeout: 8000 })

    await selectStrategy(page, 'REVIEW_ONLY')
    await page.waitForTimeout(300)

    await page.getByTestId('btn-start-multi-agent').click()
    await expect(page.getByTestId('multi-agent-run-detail')).toBeVisible({ timeout: 30000 })

    await approveAllWaitingTools(page)

    // Navigate to artifacts tab
    await page.locator('.sr-item').filter({ hasText: '产物' }).click()

    // Click patch proposal artifact tab
    const patchBtn = page.locator('.artifact-tabs button').filter({ hasText: /Mock Patch Proposal/ })
    await expect(patchBtn.first()).toBeVisible({ timeout: 10000 })
    await patchBtn.first().click()
    await page.waitForTimeout(500)

    // Verify panel rendered
    await expect(page.getByTestId('patch-review-panel')).toBeVisible({ timeout: 5000 })

    // Interact with checklist
    await page.getByTestId('patch-review-check-matches').locator('.el-checkbox').click()
    await page.getByTestId('patch-review-check-sensitive').locator('.el-checkbox').click()
    await page.getByTestId('patch-review-check-ready').locator('.el-checkbox').click()
    await page.getByTestId('patch-review-safety-confirmed').locator('.el-checkbox').click()
    await page.getByTestId('patch-review-comment').fill('E2E JS error check')

    // Submit decision
    await page.getByTestId('btn-accept-patch-plan').click()
    await expect(page.getByTestId('patch-review-status')).toBeVisible({ timeout: 10000 })

    expect(jsErrors).toEqual([])
  })
})
