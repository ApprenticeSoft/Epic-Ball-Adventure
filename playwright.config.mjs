import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env.WEB_BASE_URL || 'http://127.0.0.1:8091';
const localWebServer = process.env.WEB_BASE_URL ? undefined : {
  command: 'python3 -m http.server 8091 --bind 127.0.0.1 --directory html/build/dist',
  url: baseURL,
  reuseExistingServer: !process.env.CI,
  timeout: 10000
};

export default defineConfig({
  testDir: './tests/web',
  timeout: 60000,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL,
    browserName: 'chromium',
    trace: 'retain-on-failure'
  },
  ...(localWebServer ? { webServer: localWebServer } : {}),
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
