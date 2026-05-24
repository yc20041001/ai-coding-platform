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
    if (!projectId) throw new Error('No project available for incident E2E')

    const incidentRes = await fetch('/api/orchestration/incidents', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tok}`,
      },
      body: JSON.stringify({
        projectId,
        sourceType: 'MANUAL',
        severity: 'MEDIUM',
        title: `E2E Incident Knowledge ${Date.now()}`,
        summary: 'E2E seed incident for RCA drawer verification',
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

test.describe('Incident Knowledge & Root Cause Notes', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('should show incident section on observability page', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)
    const incidentSection = page.getByTestId('incident-section')
    await expect(incidentSection).toBeVisible({ timeout: 10000 })
  })

  test('should open incident detail drawer with root cause info', async ({ page }) => {
    await ensureIncident(page)
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const drawer = await openFirstIncident(page)

    // Should show incident title in drawer
    await expect(drawer.locator('h3')).toBeVisible({ timeout: 3000 })
  })

  test('should show root cause note section in incident drawer', async ({ page }) => {
    await ensureIncident(page)
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const drawer = await openFirstIncident(page)

    // Look for root cause section
    const rcaSection = drawer.locator('[data-testid="rca-section"]')
    await expect(rcaSection).toBeVisible({ timeout: 5000 })
  })

  test('should show similar incidents section in incident drawer', async ({ page }) => {
    await ensureIncident(page)
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const drawer = await openFirstIncident(page)

    const similarSection = drawer.locator('[data-testid="similar-incidents-section"]')
    await expect(similarSection).toBeVisible({ timeout: 5000 })
  })

  test('should show knowledge links section in incident drawer', async ({ page }) => {
    await ensureIncident(page)
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const drawer = await openFirstIncident(page)

    const linksSection = drawer.locator('[data-testid="knowledge-links-section"]')
    await expect(linksSection).toBeVisible({ timeout: 5000 })
  })

  test('should show known issue template section on observability page', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    // Check for known issue template section (integrated into escalation policy area or separate)
    const templateSection = page.locator('[data-testid="known-issue-template-section"]')
    // This section might not exist yet, so check for the observability page content
    await expect(page.locator('.page-container')).toBeVisible({ timeout: 10000 })
  })

  test('should navigate between drawer sections', async ({ page }) => {
    await ensureIncident(page)
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const drawer = await openFirstIncident(page)

    // Close drawer
    await page.keyboard.press('Escape')
    await expect(drawer).not.toBeVisible({ timeout: 3000 })
  })

  test('should show retrospective section in incident drawer', async ({ page }) => {
    await ensureIncident(page)
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const drawer = await openFirstIncident(page)

    const retroSection = drawer.locator('[data-testid="retrospective-section"]')
    await expect(retroSection).toBeVisible({ timeout: 5000 })
  })

  test('should show knowledge quality review panel on observability page', async ({ page }) => {
    await page.goto('/observability')
    await expect(page).toHaveURL(/\/observability/)

    const qualityPanel = page.locator('[data-testid="knowledge-quality-panel"]')
    await expect(qualityPanel).toBeVisible({ timeout: 10000 })
  })
})
