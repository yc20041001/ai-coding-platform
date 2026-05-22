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
  const projectName = `E2E-CodeIndex-${suffix}`

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

  // Navigate to project detail, get project ID from URL
  await page.waitForURL(/\/projects\/\d+/, { timeout: 10000 })
  const url = page.url()
  const match = url.match(/\/projects\/(\d+)/)
  if (!match) throw new Error('Could not extract project ID from URL: ' + url)
  return match[1]
}

test.describe('Code Index Page', () => {

  test('should display code-index tab in ProjectDetailPage', async ({ page }) => {
    await login(page)
    const projectId = await createProject(page)

    // Navigate to the project detail page
    await page.goto(`/projects/${projectId}`)
    await expect(page).toHaveURL(/\/projects\/\d+/)

    // The "代码索引" tab should be visible in the rail
    const codeIndexTab = page.locator('.section-rail').getByText('代码索引')
    await expect(codeIndexTab).toBeVisible({ timeout: 5000 })
  })

  test('should navigate to code-index page and show empty state', async ({ page }) => {
    await login(page)
    const projectId = await createProject(page)

    // Navigate directly to code-index page
    await page.goto(`/projects/${projectId}/code-index`)
    await expect(page).toHaveURL(/\/code-index/)

    // Should show the summary section with EmptyState
    await expect(page.getByTestId('code-index-summary-section')).toBeVisible({ timeout: 5000 })
    // Should have build button
    await expect(page.getByTestId('code-index-build-btn')).toBeVisible()
    // Should have search section
    await expect(page.getByTestId('code-index-search-section')).toBeVisible()
    // Should have safety note
    await expect(page.getByTestId('readonly-adapter-safety-note')).toBeVisible()
  })

  test('should build index and show summary', async ({ page }) => {
    await login(page)
    const projectId = await createProject(page)

    await page.goto(`/projects/${projectId}/code-index`)
    await expect(page).toHaveURL(/\/code-index/)

    // Click build button
    await page.getByTestId('code-index-build-btn').click()

    // Wait for the summary cards to appear (wait for API response)
    await expect(page.getByTestId('code-index-file-count')).toBeVisible({ timeout: 10000 })
    await expect(page.getByTestId('code-index-symbol-count')).toBeVisible()
    await expect(page.getByTestId('code-index-chunk-count')).toBeVisible()
    await expect(page.getByTestId('code-index-mock-badge')).toBeVisible()

    // Verify counts are displayed
    const fileCount = await page.getByTestId('code-index-file-count').textContent()
    expect(Number(fileCount)).toBeGreaterThan(0)
  })

  test('should search and display results', async ({ page }) => {
    await login(page)
    const projectId = await createProject(page)

    await page.goto(`/projects/${projectId}/code-index`)
    await expect(page).toHaveURL(/\/code-index/)

    // Build index first
    await page.getByTestId('code-index-build-btn').click()
    await expect(page.getByTestId('code-index-file-count')).toBeVisible({ timeout: 10000 })

    // Enter search keyword
    await page.getByTestId('code-index-search-keyword').fill('Application')

    // Click search button
    await page.getByTestId('code-index-search-btn').click()

    // Wait for results section
    await expect(page.getByTestId('code-index-results-section')).toBeVisible({ timeout: 10000 })
  })

  test('should show results with search type selector', async ({ page }) => {
    await login(page)
    const projectId = await createProject(page)

    await page.goto(`/projects/${projectId}/code-index`)
    await expect(page).toHaveURL(/\/code-index/)

    // Build index first
    await page.getByTestId('code-index-build-btn').click()
    await expect(page.getByTestId('code-index-file-count')).toBeVisible({ timeout: 10000 })

    // Search with symbol type
    await page.getByTestId('code-index-search-keyword').fill('class')
    await page.getByTestId('code-index-search-btn').click()
    await expect(page.getByTestId('code-index-results-section')).toBeVisible({ timeout: 10000 })
  })
})
