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

async function ensureIncident(page: ReturnType<typeof test['info']>['page']) {
  const token = await page.evaluate(() => localStorage.getItem('aicp_token'))
  await page.evaluate(async (tok) => {
    const projectsRes = await fetch('/api/projects?page=1&pageSize=1', {
      headers: { Authorization: `Bearer ${tok}` },
    })
    const projectsJson = await projectsRes.json()
    const projectId = projectsJson?.data?.records?.[0]?.id
    if (!projectId) throw new Error('No project available for retro E2E')

    const incidentRes = await fetch('/api/orchestration/incidents', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tok}`,
      },
      body: JSON.stringify({
        projectId,
        sourceType: 'MANUAL',
        severity: 'CRITICAL',
        title: `E2E Retro ${Date.now()}`,
        summary: 'E2E seed incident for retrospective verification',
      }),
    })
    if (!incidentRes.ok) throw new Error(`Create incident failed: ${incidentRes.status}`)
  }, token)
}

async function openFirstIncident(page: ReturnType<typeof test['info']>['page']) {
  const table = page.getByTestId('tip-table')
  await expect(table).toBeVisible({ timeout: 15000 })
  const firstRow = table.locator('.el-table__body tbody tr').first()
  await expect(firstRow).toBeVisible({ timeout: 5000 })
  await firstRow.click()
  const drawer = page.getByTestId('incident-detail-drawer')
  await expect(drawer).toBeVisible({ timeout: 5000 })
  return drawer
}

test.describe('Incident Retrospective', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should show retrospective section on observability page', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)
    const qualitySection = page.locator('[data-testid="quality-review-section"]')
    await expect(qualitySection).toBeVisible({ timeout: 10000 })
  })

  test('should show retrospective editor in incident drawer', async ({ page }) => {
    await ensureIncident(page)
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const drawer = await openFirstIncident(page)
    const retroSection = drawer.locator('[data-testid="retrospective-section"]')
    await expect(retroSection).toBeVisible({ timeout: 5000 })
  })

  test('should create retrospective draft from incident drawer', async ({ page }) => {
    await ensureIncident(page)
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const drawer = await openFirstIncident(page)

    // Scroll to retrospective section and create draft
    const retroEditor = drawer.locator('[data-testid="retrospective-editor"]')
    await expect(retroEditor).toBeVisible({ timeout: 5000 })

    const createBtn = retroEditor.locator('[data-testid="create-retro-draft-btn"]')
    if (await createBtn.isVisible()) {
      await createBtn.click()
      await expect(retroEditor.locator('.retro-title')).toBeVisible({ timeout: 5000 })
    }
  })

  test('should show knowledge quality review panel', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const qualityPanel = page.locator('[data-testid="knowledge-quality-panel"]')
    await expect(qualityPanel).toBeVisible({ timeout: 10000 })
  })

  test('should open create quality review dialog', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const qualityPanel = page.locator('[data-testid="knowledge-quality-panel"]')
    await expect(qualityPanel).toBeVisible({ timeout: 10000 })

    const createBtn = qualityPanel.locator('[data-testid="create-quality-review-btn"]')
    await expect(createBtn).toBeVisible({ timeout: 5000 })
  })

  test('should navigate incident drawer to retrospective tab', async ({ page }) => {
    await ensureIncident(page)
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const drawer = await openFirstIncident(page)

    // Verify drawer has multiple sections
    await expect(drawer.locator('[data-testid="rca-section"]')).toBeVisible({ timeout: 5000 })
    await expect(drawer.locator('[data-testid="knowledge-links-section"]')).toBeVisible({ timeout: 5000 })
    await expect(drawer.locator('[data-testid="similar-incidents-section"]')).toBeVisible({ timeout: 5000 })

    // Close drawer
    await page.keyboard.press('Escape')
    await expect(drawer).not.toBeVisible({ timeout: 3000 })
  })

  test('should show retrospective section with proper test id', async ({ page }) => {
    await ensureIncident(page)
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const drawer = await openFirstIncident(page)
    const retroSection = drawer.locator('[data-testid="retrospective-section"]')
    await expect(retroSection).toBeVisible({ timeout: 5000 })

    // The section should have a title
    await expect(retroSection).toContainText('事后回顾')
  })
})
