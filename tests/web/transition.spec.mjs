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
    await expect.poll(async () => await screenshotHasBlackCompletionScreen(page), {
      timeout: 10000,
      message: 'Expected game completion to display text over a black screen.'
    }).toBe(true);

    const gameStartCount = (await getDebugEvents(page))
      .filter(line => line.includes('GameScreen construct begin level=1')).length;
    await returnFromCompletion(page, testInfo.project.name);
    await waitForDebugEvent(page, logs, 'return to main menu after game complete', 10000);
    await expect.poll(async () => await screenshotMostlyBlack(page), {
      timeout: 10000,
      message: 'Expected the main menu to replace the black completion screen.'
    }).toBe(false);
    const afterReturnGameStartCount = (await getDebugEvents(page))
      .filter(line => line.includes('GameScreen construct begin level=1')).length;
    expect(afterReturnGameStartCount).toBe(gameStartCount);

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

test('responsive UI elements fit portrait and landscape screens', async ({ page }, testInfo) => {
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
    await page.goto('/?ballDebug=1&ballDebugRestartOverlay=1');

    await waitForDebugEvent(page, logs, 'loading logo layout', 10000);
    await waitForDebugEvent(page, logs, 'main menu layout', 10000);

    const loadingLayout = parseLayoutEvent(await latestDebugEvent(page, 'loading logo layout'));
    assertBoundsFit(loadingLayout.screen, loadingLayout.bounds);
    assertCentered(loadingLayout.screen, loadingLayout.bounds, 2);

    const menuLayout = parseMenuLayoutEvent(await latestDebugEvent(page, 'main menu layout'));
    const expectedStartText = testInfo.project.name.includes('mobile') ? 'Touch to Start' : 'Press F to start';
    expect(menuLayout.startText).toBe(expectedStartText);
    assertBoundsFit(menuLayout.screen, menuLayout.startBounds);
    assertHorizontallyCentered(menuLayout.screen, menuLayout.startBounds, 2);
    if(testInfo.project.name.includes('mobile')){
      expect(menuLayout.startBounds.height).toBeLessThanOrEqual(menuLayout.screen.height * 0.055);
    }
    const startCenterY = menuLayout.startBounds.y + menuLayout.startBounds.height / 2;
    const expectedStartCenterY = menuLayout.titleBounds.y / 2;
    expect(Math.abs(startCenterY - expectedStartCenterY)).toBeLessThanOrEqual(Math.max(3, menuLayout.screen.height * 0.03));

    await startGame(page, testInfo.project.name);
    await waitForDebugEvent(page, logs, 'game camera layout', 10000);
    const initialCameraLayout = parseCameraLayoutEvent(await latestDebugEvent(page, 'game camera layout'));
    await waitForDebugEvent(page, logs, 'restart label layout', 10000);
    const portraitRestartLayout = parseLayoutEvent(await latestDebugEvent(page, 'restart label layout'));
    assertBoundsFit(portraitRestartLayout.screen, portraitRestartLayout.bounds);
    assertCentered(portraitRestartLayout.screen, portraitRestartLayout.bounds, 3);

    const viewport = page.viewportSize();
    await page.setViewportSize({ width: viewport.height, height: viewport.width });
    await waitForRestartScreenChange(page, portraitRestartLayout.screen, 10000);
    const rotatedCameraLayout = parseCameraLayoutEvent(await latestDebugEvent(page, 'game camera layout'));
    if(testInfo.project.name.includes('mobile')){
      const zoomRatio = rotatedCameraLayout.pixelsPerWorld / initialCameraLayout.pixelsPerWorld;
      expect(Math.abs(zoomRatio - 1)).toBeLessThanOrEqual(0.04);
    }
    const landscapeRestartLayout = parseLayoutEvent(await latestDebugEvent(page, 'restart label layout'));
    assertBoundsFit(landscapeRestartLayout.screen, landscapeRestartLayout.bounds);
    assertCentered(landscapeRestartLayout.screen, landscapeRestartLayout.bounds, 3);

    expect(errors, logs.join('\n')).toEqual([]);
  }
  finally {
    await testInfo.attach('console.log', {
      body: logs.join('\n'),
      contentType: 'text/plain'
    });
    await testInfo.attach('debug-events.log', {
      body: (await getDebugEvents(page)).join('\n'),
      contentType: 'text/plain'
    });
  }
});

test('desktop editor opens and returns from playtest with Escape', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name.includes('mobile'), 'Editor is desktop-only.');

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
    await page.goto('/?ballDebug=1');
    await waitForDebugEvent(page, logs, 'main menu layout', 10000);
    await page.bringToFront();
    await page.locator('canvas').click();
    await page.waitForTimeout(300);
    await page.keyboard.press('E');
    await waitForDebugEvent(page, logs, 'level editor opened', 10000);
    await page.keyboard.press('P');
    await waitForDebugEvent(page, logs, 'level editor playtest start', 10000);
    await waitForDebugEvent(page, logs, 'loaded editor test map', 10000);
    await page.keyboard.press('Escape');
    await waitForDebugEvent(page, logs, 'return to editor from test', 10000);
    expect(errors, logs.join('\n')).toEqual([]);
  }
  finally {
    await testInfo.attach('console.log', {
      body: logs.join('\n'),
      contentType: 'text/plain'
    });
    await testInfo.attach('debug-events.log', {
      body: (await getDebugEvents(page)).join('\n'),
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

async function latestDebugEvent(page, needle) {
  const debugEvents = await getDebugEvents(page);
  for(let i = debugEvents.length - 1; i >= 0; i--){
    if(debugEvents[i].includes(needle))
      return debugEvents[i];
  }
  throw new Error(`Missing debug event "${needle}". Events:\n${debugEvents.join('\n')}`);
}

async function waitForRestartScreenChange(page, previousScreen, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  while(Date.now() < deadline){
    const latest = await latestDebugEvent(page, 'restart label layout');
    const layout = parseLayoutEvent(latest);
    if(Math.abs(layout.screen.width - previousScreen.width) > 1
        || Math.abs(layout.screen.height - previousScreen.height) > 1)
      return;
    await page.waitForTimeout(100);
  }
  const debugEvents = await getDebugEvents(page);
  throw new Error(`Timed out waiting for restart layout to resize. Events:\n${debugEvents.join('\n')}`);
}

async function startGame(page, projectName) {
  if(projectName.includes('mobile')){
    const viewport = page.viewportSize();
    await page.touchscreen.tap(viewport.width / 2, viewport.height / 2);
    return;
  }
  await page.keyboard.press('F');
}

async function returnFromCompletion(page, projectName) {
  if(projectName.includes('mobile')){
    const viewport = page.viewportSize();
    await page.touchscreen.tap(viewport.width / 2, viewport.height / 2);
    return;
  }
  await page.keyboard.press('Space');
}

function parseLayoutEvent(event) {
  const match = event.match(/screen=([\d.]+)x([\d.]+).*bounds=([\d.-]+),([\d.-]+),([\d.-]+),([\d.-]+)/);
  if(!match)
    throw new Error(`Cannot parse layout event: ${event}`);
  return {
    screen: { width: Number(match[1]), height: Number(match[2]) },
    bounds: {
      x: Number(match[3]),
      y: Number(match[4]),
      width: Number(match[5]),
      height: Number(match[6])
    }
  };
}

function parseMenuLayoutEvent(event) {
  const match = event.match(/screen=([\d.]+)x([\d.]+) startText=(.*) titleBounds=([\d.-]+),([\d.-]+),([\d.-]+),([\d.-]+) startBounds=([\d.-]+),([\d.-]+),([\d.-]+),([\d.-]+)/);
  if(!match)
    throw new Error(`Cannot parse menu layout event: ${event}`);
  return {
    screen: { width: Number(match[1]), height: Number(match[2]) },
    startText: match[3],
    titleBounds: {
      x: Number(match[4]),
      y: Number(match[5]),
      width: Number(match[6]),
      height: Number(match[7])
    },
    startBounds: {
      x: Number(match[8]),
      y: Number(match[9]),
      width: Number(match[10]),
      height: Number(match[11])
    }
  };
}

function parseCameraLayoutEvent(event) {
  const match = event.match(/screen=([\d.]+)x([\d.]+) viewport=([\d.-]+)x([\d.-]+) pixelsPerWorld=([\d.-]+) mobile=(true|false)/);
  if(!match)
    throw new Error(`Cannot parse camera layout event: ${event}`);
  return {
    screen: { width: Number(match[1]), height: Number(match[2]) },
    viewport: { width: Number(match[3]), height: Number(match[4]) },
    pixelsPerWorld: Number(match[5]),
    mobile: match[6] === 'true'
  };
}

function assertBoundsFit(screen, bounds) {
  expect(bounds.x).toBeGreaterThanOrEqual(-1);
  expect(bounds.y).toBeGreaterThanOrEqual(-1);
  expect(bounds.x + bounds.width).toBeLessThanOrEqual(screen.width + 1);
  expect(bounds.y + bounds.height).toBeLessThanOrEqual(screen.height + 1);
}

function assertCentered(screen, bounds, tolerance) {
  assertHorizontallyCentered(screen, bounds, tolerance);
  const centerY = bounds.y + bounds.height / 2;
  expect(Math.abs(centerY - screen.height / 2)).toBeLessThanOrEqual(tolerance);
}

function assertHorizontallyCentered(screen, bounds, tolerance) {
  const centerX = bounds.x + bounds.width / 2;
  expect(Math.abs(centerX - screen.width / 2)).toBeLessThanOrEqual(tolerance);
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

async function screenshotHasBlackCompletionScreen(page) {
  const stats = await sampleScreenshot(page);
  return stats.blackPixels > stats.samples * 0.65 && stats.visiblePixels > 50;
}

async function screenshotMostlyBlack(page) {
  const stats = await sampleScreenshot(page);
  return stats.blackPixels > stats.samples * 0.65;
}

async function sampleScreenshot(page) {
  const png = PNG.sync.read(await page.screenshot());
  const stride = Math.max(1, Math.floor(Math.sqrt((png.width * png.height) / 20000)));
  let blackPixels = 0;
  let visiblePixels = 0;
  let samples = 0;

  for(let y = 0; y < png.height; y += stride){
    for(let x = 0; x < png.width; x += stride){
      const index = (png.width * y + x) << 2;
      if(png.data[index + 3] === 0)
        continue;
      samples++;

      const red = png.data[index];
      const green = png.data[index + 1];
      const blue = png.data[index + 2];
      if(red <= 10 && green <= 10 && blue <= 10)
        blackPixels++;
      if(red > 20 || green > 20 || blue > 20)
        visiblePixels++;
    }
  }

  return { blackPixels, visiblePixels, samples };
}
