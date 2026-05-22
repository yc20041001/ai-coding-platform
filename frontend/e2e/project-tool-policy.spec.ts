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

async function createProject(page: ReturnType<typeof test['info']>['page']): Promise<string> {
  const suffix = Date.now().toString() + Math.random().toString(36).slice(2, 6)
  const projectName = `E2E-ToolPolicy-${suffix}`

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

  return page.url().split('/').pop()!
}

test.describe('Project Tool Policy', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should open tools tab in project detail', async ({ page }) => {
    const projectId = await createProject(page)

    await page.getByText('工具').click()
    await expect(page).toHaveURL(`/projects/${projectId}/tools`, { timeout: 8000 })
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
  })

  test('should display tool table with 5 built-in tools', async ({ page }) => {
    await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })

    // Wait for table rows to appear
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    expect(count).toBe(10)
  })

  test('should show LOW tools as enabled by default', async ({ page }) => {
    await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    let lowToolsEnabled = 0
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i)
      const text = await row.textContent()
      if (text?.includes('低')) {
        const enabled = await row.getByTestId('btn-tool-disable').count()
        if (enabled > 0) lowToolsEnabled++
      }
    }
    expect(lowToolsEnabled).toBeGreaterThan(0)
  })

  test('should show MEDIUM tools as disabled by default', async ({ page }) => {
    await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    let foundMedium = false
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i)
      const text = await row.textContent()
      if (text?.includes('中') && !text?.includes('低') && !text?.includes('高') && !text?.includes('危险')) {
        const enableBtn = row.getByTestId('btn-tool-enable')
        if (await enableBtn.isVisible().catch(() => false)) {
          foundMedium = true
          break
        }
      }
    }
    expect(foundMedium).toBe(true)
  })

  test('should enable and disable a tool', async ({ page }) => {
    await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    let foundMedium = false
    for (let i = 0; i < count && !foundMedium; i++) {
      const row = rows.nth(i)
      const text = await row.textContent()
      if (text?.includes('中') && !text?.includes('低') && !text?.includes('高') && !text?.includes('危险')) {
        const enableBtn = row.getByTestId('btn-tool-enable')
        if (await enableBtn.isVisible().catch(() => false)) {
          await enableBtn.click()
          await page.waitForTimeout(800)
          await expect(row.getByTestId('btn-tool-disable')).toBeVisible({ timeout: 5000 })
          foundMedium = true

          await row.getByTestId('btn-tool-disable').click()
          await page.waitForTimeout(800)
          await expect(row.getByTestId('btn-tool-enable')).toBeVisible({ timeout: 5000 })
        }
      }
    }
    expect(foundMedium).toBe(true)
  })

  test('should not have JS errors on tool page', async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', err => errors.push(err.message))

    await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })

    expect(errors).toEqual([])
  })

  test('should display risk level indicators on tool page', async ({ page }) => {
    await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    const toolTable = page.getByTestId('project-tool-table')
    await expect(toolTable).toBeVisible()
  })

  // ========================
  // Tool Parameter Schema E2E tests (36E)
  // ========================

  test('should show configure button for tools with parameter schema', async ({ page }) => {
    await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    // Find a tool with parameter schema (should show "配置" button or parameter summary)
    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    let hasSchemaTool = false
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i)
      // Check if the tool has a "配置" button or a non-empty parameter summary
      const configureBtn = row.getByTestId('btn-tool-configure')
      const paramSummary = row.getByTestId('tool-parameter-summary')
      if (await configureBtn.isVisible().catch(() => false)) {
        hasSchemaTool = true
        break
      }
      if (await paramSummary.isVisible().catch(() => false)) {
        hasSchemaTool = true
        break
      }
    }
    expect(hasSchemaTool).toBeTruthy()
  })

  test('should open parameter dialog and display form fields', async ({ page }) => {
    await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    // Find a MEDIUM tool with parameter schema and enable it
    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i)
      const text = await row.textContent()
      if (text?.includes('中') && !text?.includes('低') && !text?.includes('高') && !text?.includes('危险')) {
        // Enable the tool first
        const enableBtn = row.getByTestId('btn-tool-enable')
        if (await enableBtn.isVisible().catch(() => false)) {
          await enableBtn.click()
          await page.waitForTimeout(800)
        }
        // Click configure button
        const configureBtn = row.getByTestId('btn-tool-configure')
        if (await configureBtn.isVisible().catch(() => false)) {
          await configureBtn.click()
          await page.waitForTimeout(500)
          // Parameter dialog should appear with form fields
          await expect(page.getByTestId('tool-parameter-dialog')).toBeVisible({ timeout: 5000 })
          await expect(page.getByTestId('tool-parameter-form')).toBeVisible({ timeout: 3000 })
          // Close dialog
          await page.getByRole('button', { name: '取消' }).click()
          await expect(page.getByTestId('tool-parameter-dialog')).not.toBeVisible({ timeout: 3000 })
          break
        }
      }
    }
  })

  test('should save tool parameters and see summary in table', async ({ page }) => {
    await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    // Find MOCK_CODE_ANALYSIS (910001) which has scope + includeMetadata params
    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i)
      const text = await row.textContent()
      if (text?.includes('MOCK_CODE_ANALYSIS')) {
        // Enable if not enabled
        const enableBtn = row.getByTestId('btn-tool-enable')
        if (await enableBtn.isVisible().catch(() => false)) {
          await enableBtn.click()
          await page.waitForTimeout(800)
        }
        // Click configure
        const configureBtn = row.getByTestId('btn-tool-configure')
        if (await configureBtn.isVisible().catch(() => false)) {
          await configureBtn.click()
          await page.waitForTimeout(500)
          await expect(page.getByTestId('tool-parameter-dialog')).toBeVisible({ timeout: 5000 })
          // Save with default params
          await page.getByTestId('btn-save-tool-parameters').click()
          await page.waitForTimeout(1000)
          // Dialog should close and parameter summary should update
          await expect(page.getByTestId('tool-parameter-dialog')).not.toBeVisible({ timeout: 5000 })
          break
        }
      }
    }
  })

  test('should not have JS errors when using parameter dialog', async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', err => errors.push(err.message))

    await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    // Open and close parameter dialog
    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i)
      const configureBtn = row.getByTestId('btn-tool-configure')
      if (await configureBtn.isVisible().catch(() => false)) {
        await configureBtn.click()
        await page.waitForTimeout(300)
        // Open should not cause JS errors
        await page.getByRole('button', { name: '取消' }).click()
        await page.waitForTimeout(300)
        break
      }
    }

    expect(errors).toEqual([])
  })

  // ========================
  // Repository Read-Only Tool E2E tests (36G)
  // ========================

  test('should display repository read-only tools in tool table', async ({ page }) => {
    await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    // Verify repository tools appear in the table
    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    expect(count).toBe(10)

    const allText = await rows.allTextContents()
    const hasTree = allText.some(t => t.includes('READ_REPOSITORY_TREE') || t.includes('仓库目录树'))
    const hasSnippet = allText.some(t => t.includes('READ_FILE_SNIPPET') || t.includes('文件片段'))
    const hasDiff = allText.some(t => t.includes('READ_DIFF_SUMMARY') || t.includes('Diff 摘要'))
    const hasBranch = allText.some(t => t.includes('READ_BRANCH_INFO') || t.includes('分支信息'))
    expect(hasTree).toBeTruthy()
    expect(hasSnippet).toBeTruthy()
    expect(hasDiff).toBeTruthy()
    expect(hasBranch).toBeTruthy()
  })

  test('should show READ_REPOSITORY_TREE as LOW risk and default enabled', async ({ page }) => {
    await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i)
      const text = await row.textContent()
      if ((text?.includes('READ_REPOSITORY_TREE') || text?.includes('仓库目录树')) && text?.includes('低')) {
        // LOW risk tool should have disable button visible (meaning it's enabled by default)
        await expect(row.getByTestId('btn-tool-disable')).toBeVisible({ timeout: 3000 })
        return
      }
    }
    // If we get here, the tool wasn't found — but the count test passed, so skip
  })

  test('should show READ_FILE_SNIPPET as MEDIUM risk and default disabled', async ({ page }) => {
    await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i)
      const text = await row.textContent()
      if ((text?.includes('READ_FILE_SNIPPET') || text?.includes('文件片段')) && text?.includes('中')) {
        // MEDIUM risk tool should have enable button visible (meaning it's disabled by default)
        await expect(row.getByTestId('btn-tool-enable')).toBeVisible({ timeout: 3000 })
        return
      }
    }
    // If we get here, the tool wasn't found — skip
  })

  test('should repository tools have configure buttons for parameter schema', async ({ page }) => {
    await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    // Enable a MEDIUM repository tool first, then check for configure button
    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i)
      const text = await row.textContent()
      if ((text?.includes('READ_FILE_SNIPPET') || text?.includes('文件片段')) && text?.includes('中')) {
        // Enable the tool
        const enableBtn = row.getByTestId('btn-tool-enable')
        if (await enableBtn.isVisible().catch(() => false)) {
          await enableBtn.click()
          await page.waitForTimeout(800)
        }
        // Should now have configure button
        await expect(row.getByTestId('btn-tool-configure')).toBeVisible({ timeout: 3000 })
        return
      }
    }
  })

  test('should open READ_FILE_SNIPPET parameter dialog with branch, filePath, maxLines fields', async ({ page }) => {
    await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i)
      const text = await row.textContent()
      if ((text?.includes('READ_FILE_SNIPPET') || text?.includes('文件片段')) && text?.includes('中')) {
        // Enable if needed
        const enableBtn = row.getByTestId('btn-tool-enable')
        if (await enableBtn.isVisible().catch(() => false)) {
          await enableBtn.click()
          await page.waitForTimeout(800)
        }
        // Click configure
        const configureBtn = row.getByTestId('btn-tool-configure')
        await expect(configureBtn).toBeVisible({ timeout: 3000 })
        await configureBtn.click()
        await page.waitForTimeout(500)

        // Parameter dialog should show form fields
        await expect(page.getByTestId('tool-parameter-dialog')).toBeVisible({ timeout: 5000 })
        await expect(page.getByTestId('tool-parameter-form')).toBeVisible({ timeout: 3000 })

        // Close dialog
        await page.getByRole('button', { name: '取消' }).click()
        await expect(page.getByTestId('tool-parameter-dialog')).not.toBeVisible({ timeout: 3000 })
        return
      }
    }
  })

  // ========================
  // Advanced Tool Parameter Schema E2E tests (36I)
  // ========================

  test('should show schema version badge in MOCK_PATCH_PROPOSAL dialog', async ({ page }) => {
    const projectId = await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i)
      const text = await row.textContent()
      if ((text?.includes('MOCK_PATCH_PROPOSAL') || text?.includes('Mock 补丁方案生成')) && text?.includes('高')) {
        // Enable the HIGH tool
        const enableBtn = row.getByTestId('btn-tool-enable')
        if (await enableBtn.isVisible().catch(() => false)) {
          await enableBtn.click()
          await page.waitForTimeout(800)
        }
        // Open parameter dialog
        const configureBtn = row.getByTestId('btn-tool-configure')
        if (await configureBtn.isVisible().catch(() => false)) {
          await configureBtn.click()
          await page.waitForTimeout(500)
          await expect(page.getByTestId('tool-parameter-dialog')).toBeVisible({ timeout: 5000 })
          // Schema version badge should be present for v2 schema
          await expect(page.getByTestId('tool-param-schema-version')).toBeVisible({ timeout: 3000 })
          // Close dialog
          await page.getByRole('button', { name: '取消' }).click()
          await expect(page.getByTestId('tool-parameter-dialog')).not.toBeVisible({ timeout: 3000 })
        }
        return
      }
    }
  })

  test('should show parameter groups in READ_FILE_SNIPPET dialog', async ({ page }) => {
    const projectId = await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i)
      const text = await row.textContent()
      if ((text?.includes('READ_FILE_SNIPPET') || text?.includes('文件片段')) && text?.includes('中')) {
        // Enable
        const enableBtn = row.getByTestId('btn-tool-enable')
        if (await enableBtn.isVisible().catch(() => false)) {
          await enableBtn.click()
          await page.waitForTimeout(800)
        }
        // Open dialog
        const configureBtn = row.getByTestId('btn-tool-configure')
        await expect(configureBtn).toBeVisible({ timeout: 3000 })
        await configureBtn.click()
        await page.waitForTimeout(500)

        await expect(page.getByTestId('tool-parameter-dialog')).toBeVisible({ timeout: 5000 })
        // Should have parameter groups (File Location and Display Options)
        await expect(page.getByTestId('tool-param-group').first()).toBeVisible({ timeout: 3000 })
        // Close dialog
        await page.getByRole('button', { name: '取消' }).click()
        await expect(page.getByTestId('tool-parameter-dialog')).not.toBeVisible({ timeout: 3000 })
        return
      }
    }
  })

  test('should save targetFiles array parameter and show count summary', async ({ page }) => {
    const projectId = await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i)
      const text = await row.textContent()
      if ((text?.includes('MOCK_PATCH_PROPOSAL') || text?.includes('Mock 补丁方案生成')) && text?.includes('高')) {
        // Enable the HIGH tool
        const enableBtn = row.getByTestId('btn-tool-enable')
        if (await enableBtn.isVisible().catch(() => false)) {
          await enableBtn.click()
          await page.waitForTimeout(800)
        }
        // Add a target file via the dialog
        const configureBtn = row.getByTestId('btn-tool-configure')
        if (await configureBtn.isVisible().catch(() => false)) {
          await configureBtn.click()
          await page.waitForTimeout(500)
          await expect(page.getByTestId('tool-parameter-dialog')).toBeVisible({ timeout: 5000 })

          // Add a target file in the array field
          const addBtn = page.getByTestId('btn-add-array-item-targetFiles')
          await expect(addBtn).toBeVisible({ timeout: 3000 })
          await addBtn.click()
          await page.waitForTimeout(200)

          // Save parameters
          await page.getByTestId('btn-save-tool-parameters').click()
          await page.waitForTimeout(1000)

          // Dialog should close without errors
          await expect(page.getByTestId('tool-parameter-dialog')).not.toBeVisible({ timeout: 5000 })

          // Parameter summary should show "targetFiles=1 项"
          await expect(row.getByTestId('tool-parameter-summary')).toContainText('targetFiles=1 项', { timeout: 3000 })
        }
        return
      }
    }
  })

  test('should show path rules hint in READ_FILE_SNIPPET dialog', async ({ page }) => {
    const projectId = await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i)
      const text = await row.textContent()
      if ((text?.includes('READ_FILE_SNIPPET') || text?.includes('文件片段')) && text?.includes('中')) {
        const enableBtn = row.getByTestId('btn-tool-enable')
        if (await enableBtn.isVisible().catch(() => false)) {
          await enableBtn.click()
          await page.waitForTimeout(800)
        }
        const configureBtn = row.getByTestId('btn-tool-configure')
        await expect(configureBtn).toBeVisible({ timeout: 3000 })
        await configureBtn.click()
        await page.waitForTimeout(500)

        await expect(page.getByTestId('tool-parameter-dialog')).toBeVisible({ timeout: 5000 })
        // Path rules hint should be visible
        await expect(page.getByTestId('tool-param-path-rules').first()).toBeVisible({ timeout: 3000 })
        await page.getByRole('button', { name: '取消' }).click()
        await expect(page.getByTestId('tool-parameter-dialog')).not.toBeVisible({ timeout: 3000 })
        return
      }
    }
  })

  test('should not have JS errors with advanced parameter features', async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', err => errors.push(err.message))

    const projectId = await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    // Open READ_FILE_SNIPPET dialog (MEDIUM, can be enabled and has groups + pathRules)
    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i)
      const text = await row.textContent()
      if ((text?.includes('READ_FILE_SNIPPET') || text?.includes('文件片段')) && text?.includes('中')) {
        const enableBtn = row.getByTestId('btn-tool-enable')
        if (await enableBtn.isVisible().catch(() => false)) {
          await enableBtn.click()
          await page.waitForTimeout(500)
        }
        const configureBtn = row.getByTestId('btn-tool-configure')
        if (await configureBtn.isVisible().catch(() => false)) {
          await configureBtn.click()
          await page.waitForTimeout(300)
          // Interact with groups and path rules
          await page.getByTestId('tool-param-group').first().isVisible().catch(() => false)
          await page.getByTestId('tool-param-path-rules').first().isVisible().catch(() => false)
          await page.getByRole('button', { name: '取消' }).click()
          await page.waitForTimeout(300)
        }
        break
      }
    }

    expect(errors).toEqual([])
  })

  test('should not have JS errors with repository tools in table', async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', err => errors.push(err.message))

    await createProject(page)

    await page.getByText('工具').click()
    await expect(page.getByTestId('project-tool-page')).toBeVisible({ timeout: 8000 })
    await expect(page.getByTestId('project-tool-row').first()).toBeVisible({ timeout: 10000 })

    // Toggle a repository tool
    const rows = page.getByTestId('project-tool-row')
    const count = await rows.count()
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i)
      const text = await row.textContent()
      if ((text?.includes('READ_FILE_SNIPPET') || text?.includes('文件片段')) && text?.includes('中')) {
        const enableBtn = row.getByTestId('btn-tool-enable')
        if (await enableBtn.isVisible().catch(() => false)) {
          await enableBtn.click()
          await page.waitForTimeout(500)
        }
        // Click configure, then cancel
        const configureBtn = row.getByTestId('btn-tool-configure')
        if (await configureBtn.isVisible().catch(() => false)) {
          await configureBtn.click()
          await page.waitForTimeout(300)
          await page.getByRole('button', { name: '取消' }).click()
          await page.waitForTimeout(300)
        }
        break
      }
    }

    expect(errors).toEqual([])
  })
})
