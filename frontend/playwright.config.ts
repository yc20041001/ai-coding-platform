import { defineConfig } from '@playwright/test'

const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'

export default defineConfig({
  testDir: './e2e',
  timeout: 60000,
  retries: 0,
  use: {
    baseURL,
    browserName: 'chromium',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    headless: true,
  },
  webServer: {
    command: 'npm run dev -- --host 0.0.0.0',
    url: baseURL,
    reuseExistingServer: true,
    timeout: 30000,
  },
})
