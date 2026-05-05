import { expect, test } from '@playwright/test';
import { PNG } from 'pngjs';

test('auto-advances through every level without a black screen', async ({ page }, testInfo) => {
  const logs = [];
  const errors = [];

  page.on('console', message => {
    const text = message.text();
    logs.push(text);
    if (message.type() === 'error') {
      errors.push(text);
    }
  });
  page.on('pageerror', error => errors.push(error.stack || error.message));

  try {
    await page.goto('/?ballDebug=1&ballAutoAdvance=1&ballAutoAdvanceDelay=0.15&ballStartLevel=1');

    for(const level of [2, 3, 4, 5]){
      await waitForDebugEvent(page, logs, `queued level active level=${level}`, 30000);
      await expect.poll(async () => await screenshotHasNonBlackPixels(page), {
        timeout: 10000,
        message: `Expected the rendered page screenshot to contain visible non-black pixels after level ${level} loads.`
      }).toBe(true);
    }

    await waitForDebugEvent(page, logs, 'game complete level=5', 30000);
    await expect.poll(async () => await screenshotHasNonBlackPixels(page), {
      timeout: 10000,
      message: 'Expected the rendered page screenshot to remain visible after final completion.'
    }).toBe(true);

    expect(errors, logs.join('\n')).toEqual([]);
    const debugEvents = await getDebugEvents(page);
    expect(debugEvents.join('\n')).not.toContain('queued level failed');
  }
  finally {
    const debugEvents = await getDebugEvents(page);
    await testInfo.attach('console.log', {
      body: logs.join('\n'),
      contentType: 'text/plain'
    });
    await testInfo.attach('debug-events.log', {
      body: debugEvents.join('\n'),
      contentType: 'text/plain'
    });
  }
});

async function waitForDebugEvent(page, logs, needle, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  while(Date.now() < deadline){
    const debugEvents = await getDebugEvents(page);
    if(debugEvents.some(line => line.includes(needle)) || logs.some(line => line.includes(needle)))
      return;
    await page.waitForTimeout(100);
  }
  const debugEvents = await getDebugEvents(page);
  throw new Error(`Timed out waiting for "${needle}". Debug events:\n${debugEvents.join('\n')}\n\nConsole logs:\n${logs.join('\n')}`);
}

async function getDebugEvents(page) {
  return await page.evaluate(() => window.__epicBallDebugEvents || []);
}

async function screenshotHasNonBlackPixels(page) {
  const png = PNG.sync.read(await page.screenshot());
  const stride = Math.max(1, Math.floor(Math.sqrt((png.width * png.height) / 20000)));
  let visiblePixels = 0;
  let samples = 0;

  for(let y = 0; y < png.height; y += stride){
    for(let x = 0; x < png.width; x += stride){
      const index = (png.width * y + x) << 2;
      const alpha = png.data[index + 3];
      if(alpha === 0)
        continue;
      samples++;
      if(png.data[index] > 12 || png.data[index + 1] > 12 || png.data[index + 2] > 12)
        visiblePixels++;
    }
  }

  return visiblePixels > Math.max(50, samples * 0.02);
}
