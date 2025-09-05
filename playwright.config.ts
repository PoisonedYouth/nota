import { defineConfig, devices } from '@playwright/test';

/**
 * @see https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: './tests/playwright',
  /* Increase default test timeout to reduce flaky timeouts on slower environments */
  timeout: process.env.CI ? 60 * 1000 : 120 * 1000, // Shorter timeout in CI to prevent hanging
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 1 : 0, // Reduce retries in CI to prevent excessive runtime
  /* Opt out of parallel tests on CI. */
  workers: process.env.CI ? 1 : 3,
  /* Global timeout for the entire test run */
  globalTimeout: process.env.CI ? 20 * 60 * 1000 : undefined, // 20 minutes max in CI
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: process.env.CI ? 'list' : 'html',
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    /* Base URL to use in actions like `await page.goto('/')`. */
    baseURL: 'http://localhost:8080',
    /* Give navigations a bit more time in CI */
    navigationTimeout: 60 * 1000,
    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: 'on-first-retry',
  },
  /* Expect assertions timeout */
  expect: {
    timeout: 15 * 1000,
  },

  /* Configure projects for major browsers */
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },

    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },

    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },

    /* Test against mobile viewports. */
    {
      name: 'Mobile Chrome',
      use: { ...devices['Pixel 5'], hasTouch: true },
    },
    {
      name: 'Mobile Safari',
      use: { ...devices['iPhone 12'], hasTouch: true },
    },

    /* Test against branded browsers. */
    // {
    //   name: 'Microsoft Edge',
    //   use: { ...devices['Desktop Edge'], channel: 'msedge' },
    // },
    // {
    //   name: 'Google Chrome',
    //   use: { ...devices['Desktop Chrome'], channel: 'chrome' },
    // },
  ],

  /* Run your local dev server before starting the tests (only locally, CI handles server separately). */
  webServer: process.env.CI ? undefined : {
    command: './amper run',
    url: 'http://localhost:8080',
    reuseExistingServer: true,
    timeout: 180 * 1000,
    env: {
      SPRING_PROFILES_ACTIVE: 'test',
    },
  },
});
