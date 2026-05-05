import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests/web',
  timeout: 60000,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: 'http://127.0.0.1:8091',
    browserName: 'chromium',
    trace: 'retain-on-failure'
  },
  webServer: {
    command: 'python3 -m http.server 8091 --bind 127.0.0.1 --directory html/build/dist',
    url: 'http://127.0.0.1:8091',
    reuseExistingServer: !process.env.CI,
    timeout: 10000
  },
  projects: [
    {
      name: 'desktop',
      use: {
        viewport: { width: 1280, height: 720 },
        deviceScaleFactor: 1
      }
    },
    {
      name: 'mobile-portrait',
      use: {
        ...devices['Pixel 7'],
        viewport: { width: 412, height: 915 }
      }
    }
  ]
});
