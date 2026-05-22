#!/usr/bin/env node

import { chromium } from '@playwright/test';
import { PNG } from 'pngjs';
import { execFileSync } from 'node:child_process';
import { createServer } from 'node:http';
import { existsSync } from 'node:fs';
import { mkdir, readFile, stat, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

const options = parseArgs(process.argv.slice(2));
let localServer;
let browser;

try {
	const baseUrl = options.url || await startLocalServer();
	const browserResult = await runBrowserCheck(baseUrl);
	await writeJson(browserResult.outputPath, browserResult.report);
	console.log(`${browserResult.label}: ${browserResult.outputPath}`);
	if(browserResult.screenshotPath)
		console.log(`Screenshot: ${browserResult.screenshotPath}`);
}
catch(error) {
	console.error(error.message || error);
	process.exitCode = 1;
}
finally {
	if(browser)
		await browser.close();
	if(localServer)
		await new Promise(resolve => localServer.close(resolve));
}

function parseArgs(argv) {
	const parsed = {
		mode: 'benchmark',
		level: 1,
		seconds: 8,
		port: 8092,
		scene: 'game',
		viewport: { width: 1280, height: 720 },
		dist: path.join(rootDir, 'html', 'build', 'dist')
	};

	for(let i = 0; i < argv.length; i++){
		const raw = argv[i];
		const separator = raw.indexOf('=');
		const name = separator >= 0 ? raw.substring(0, separator) : raw;
		const inlineValue = separator >= 0 ? raw.substring(separator + 1) : undefined;
		const value = inlineValue !== undefined ? inlineValue : argv[i + 1];
		const consumedValue = inlineValue === undefined;
		switch(name){
			case '--mode':
				parsed.mode = requireValue(name, value);
				break;
			case '--url':
				parsed.url = requireValue(name, value);
				break;
			case '--level':
				parsed.level = parsePositiveInt(name, requireValue(name, value));
				break;
			case '--seconds':
				parsed.seconds = parsePositiveNumber(name, requireValue(name, value));
				break;
			case '--port':
				parsed.port = parsePositiveInt(name, requireValue(name, value));
				break;
			case '--scene':
				parsed.scene = requireValue(name, value);
				break;
			case '--viewport':
				parsed.viewport = parseViewport(requireValue(name, value));
				break;
			case '--dist':
				parsed.dist = path.resolve(rootDir, requireValue(name, value));
				break;
			case '--output':
				parsed.output = path.resolve(rootDir, requireValue(name, value));
				break;
			case '--screenshot':
				parsed.screenshot = path.resolve(rootDir, requireValue(name, value));
				break;
			default:
				throw new Error(`Unknown option: ${raw}`);
		}
		if(consumedValue)
			i++;
	}

	if(!['benchmark', 'screenshot', 'smoke'].includes(parsed.mode))
		throw new Error(`Unsupported --mode value: ${parsed.mode}`);
	if(!['menu', 'game', 'editor'].includes(parsed.scene))
		throw new Error(`Unsupported --scene value: ${parsed.scene}`);
	return parsed;
}

function requireValue(name, value) {
	if(value === undefined || value.startsWith('--'))
		throw new Error(`${name} requires a value`);
	return value;
}

function parsePositiveInt(name, value) {
	const parsed = Number.parseInt(value, 10);
	if(!Number.isFinite(parsed) || parsed <= 0)
		throw new Error(`${name} must be a positive integer`);
	return parsed;
}

function parsePositiveNumber(name, value) {
	const parsed = Number.parseFloat(value);
	if(!Number.isFinite(parsed) || parsed <= 0)
		throw new Error(`${name} must be a positive number`);
	return parsed;
}

function parseViewport(value) {
	const match = value.match(/^(\d+)x(\d+)$/);
	if(!match)
		throw new Error('--viewport must use WIDTHxHEIGHT, for example 1280x720');
	return {
		width: Number(match[1]),
		height: Number(match[2])
	};
}

async function startLocalServer() {
	if(!existsSync(path.join(options.dist, 'index.html')))
		throw new Error(`Missing ${path.relative(rootDir, options.dist)}/index.html. Run ./gradlew :html:dist first.`);

	localServer = createServer(async (request, response) => {
		try {
			const requestUrl = new URL(request.url || '/', `http://127.0.0.1:${options.port}`);
			let requestedPath = decodeURIComponent(requestUrl.pathname);
			if(requestedPath === '/')
				requestedPath = '/index.html';
			let filePath = path.join(options.dist, requestedPath);
			const relativePath = path.relative(options.dist, filePath);
			if(relativePath.startsWith('..') || path.isAbsolute(relativePath)){
				response.writeHead(403);
				response.end('Forbidden');
				return;
			}
			const fileStat = await stat(filePath);
			if(fileStat.isDirectory())
				filePath = path.join(filePath, 'index.html');
			const body = await readFile(filePath);
			response.writeHead(200, { 'Content-Type': mimeType(filePath) });
			response.end(body);
		}
		catch(error) {
			response.writeHead(404);
			response.end('Not found');
		}
	});

	await new Promise((resolve, reject) => {
		localServer.once('error', reject);
		localServer.listen(options.port, '127.0.0.1', resolve);
	});
	return `http://127.0.0.1:${options.port}/`;
}

function mimeType(filePath) {
	if(filePath.endsWith('.html'))
		return 'text/html; charset=utf-8';
	if(filePath.endsWith('.js'))
		return 'application/javascript; charset=utf-8';
	if(filePath.endsWith('.css'))
		return 'text/css; charset=utf-8';
	if(filePath.endsWith('.png'))
		return 'image/png';
	if(filePath.endsWith('.mp3'))
		return 'audio/mpeg';
	if(filePath.endsWith('.ogg'))
		return 'audio/ogg';
	return 'application/octet-stream';
}

async function runBrowserCheck(baseUrl) {
	browser = await chromium.launch();
	const page = await browser.newPage({ viewport: options.viewport, deviceScaleFactor: 1 });
	const logs = [];
	const errors = [];
	page.on('console', message => {
		const text = message.text();
		logs.push(text);
		if(message.type() === 'error')
			errors.push(text);
	});
	page.on('pageerror', error => errors.push(error.stack || error.message));

	if(options.mode === 'smoke')
		return await runSmoke(page, baseUrl, logs, errors);
	if(options.mode === 'screenshot')
		return await runScreenshot(page, baseUrl, logs, errors);
	return await runBenchmark(page, baseUrl, logs, errors);
}

async function runSmoke(page, baseUrl, logs, errors) {
	const rootStatus = await httpStatus(new URL('/', baseUrl).toString());
	const privacyStatus = await httpStatus(new URL('/privacy.html', baseUrl).toString());
	if(!rootStatus.ok)
		throw new Error(`Root URL failed: HTTP ${rootStatus.status}`);
	if(!privacyStatus.ok)
		throw new Error(`Privacy URL failed: HTTP ${privacyStatus.status}`);

	const startUrl = withQuery(baseUrl, {
		ballDebug: '1',
		ballStartLevel: String(options.level),
		ballResetProgress: '1'
	});
	const startNanos = process.hrtime.bigint();
	await page.goto(startUrl, { waitUntil: 'domcontentloaded' });
	await waitForDebugEvent(page, logs, 'main menu layout', 15000);
	const menuMs = elapsedMs(startNanos);
	await page.keyboard.press('F');
	await waitForDebugEvent(page, logs, `loaded tmx level=${options.level}`, 20000);
	const gameplayMs = elapsedMs(startNanos);

	const screenshotPath = options.screenshot || path.join(rootDir, 'build', 'reports', 'screenshots', 'web', 'live-smoke.png');
	await ensureParent(screenshotPath);
	const screenshot = await page.screenshot({ path: screenshotPath });
	if(!screenshotHasNonBlackPixels(screenshot))
		throw new Error('Live smoke screenshot was blank or nearly black.');
	assertNoBrowserErrors(errors, logs);

	const report = await baseReport(baseUrl, page, {
		mode: 'smoke',
		rootStatus: rootStatus.status,
		privacyStatus: privacyStatus.status,
		menuMs: round(menuMs),
		gameplayMs: round(gameplayMs),
		screenshotPath: relativeToRoot(screenshotPath),
		debugEvents: await getDebugEvents(page)
	});
	return {
		label: 'Web smoke report',
		outputPath: options.output || path.join(rootDir, 'build', 'reports', 'web-smoke', 'smoke.json'),
		screenshotPath,
		report
	};
}

async function runScreenshot(page, baseUrl, logs, errors) {
	const startUrl = screenshotUrl(baseUrl);
	await page.goto(startUrl, { waitUntil: 'domcontentloaded' });
	await prepareScene(page, logs, options.scene);

	const screenshotPath = options.screenshot || path.join(rootDir, 'build', 'reports', 'screenshots', 'web', `${options.scene}.png`);
	await ensureParent(screenshotPath);
	const screenshot = await page.screenshot({ path: screenshotPath });
	if(!screenshotHasNonBlackPixels(screenshot))
		throw new Error('Screenshot was blank or nearly black.');
	assertNoBrowserErrors(errors, logs);

	const report = await baseReport(baseUrl, page, {
		mode: 'screenshot',
		scene: options.scene,
		level: options.level,
		screenshotPath: relativeToRoot(screenshotPath),
		debugEvents: await getDebugEvents(page)
	});
	return {
		label: 'Web screenshot report',
		outputPath: options.output || path.join(rootDir, 'build', 'reports', 'web-screenshot', `${options.scene}.json`),
		screenshotPath,
		report
	};
}

async function runBenchmark(page, baseUrl, logs, errors) {
	const startUrl = withQuery(baseUrl, {
		ballDebug: '1',
		ballBenchmark: '1',
		ballFixedStep: '1',
		ballResetProgress: '1',
		ballStartLevel: String(options.level)
	});
	const startNanos = process.hrtime.bigint();
	await page.goto(startUrl, { waitUntil: 'domcontentloaded' });
	await waitForDebugEvent(page, logs, 'main menu layout', 15000);
	const menuMs = elapsedMs(startNanos);
	await waitForDebugEvent(page, logs, `loaded tmx level=${options.level}`, 20000);
	const firstPlayableFrameMs = elapsedMs(startNanos);
	await expectVisibleGameplay(page);

	const memoryBefore = await getMemory(page);
	const frameProbe = await measureAnimationFrames(page, Math.round(options.seconds * 1000));
	const memoryAfter = await getMemory(page);
	const screenshotPath = options.screenshot || path.join(rootDir, 'build', 'reports', 'screenshots', 'web', 'benchmark.png');
	await ensureParent(screenshotPath);
	const screenshot = await page.screenshot({ path: screenshotPath });
	if(!screenshotHasNonBlackPixels(screenshot))
		throw new Error('Benchmark screenshot was blank or nearly black.');
	assertNoBrowserErrors(errors, logs);

	const frameStats = summarizeFrames(frameProbe.deltas);
	const report = await baseReport(baseUrl, page, {
		mode: 'benchmark',
		level: options.level,
		seconds: options.seconds,
		scene: 'GameScreen',
		menuMs: round(menuMs),
		firstPlayableFrameMs: round(firstPlayableFrameMs),
		frames: frameProbe.count,
		durationMs: round(frameProbe.durationMs),
		averageFrameMs: round(frameStats.average),
		p95FrameMs: round(frameStats.p95),
		worstFrameMs: round(frameStats.worst),
		memoryBefore,
		memoryAfter,
		screenshotPath: relativeToRoot(screenshotPath),
		debugEvents: await getDebugEvents(page)
	});
	return {
		label: 'Web benchmark report',
		outputPath: options.output || path.join(rootDir, 'build', 'reports', 'web-benchmark', 'benchmark.json'),
		screenshotPath,
		report
	};
}

function screenshotUrl(baseUrl) {
	const params = {
		ballDebug: '1',
		ballStartLevel: String(options.level),
		ballResetProgress: '1'
	};
	if(options.scene === 'editor')
		params.ballStartEditor = '1';
	return withQuery(baseUrl, params);
}

async function prepareScene(page, logs, scene) {
	if(scene === 'menu'){
		await waitForDebugEvent(page, logs, 'main menu layout', 15000);
		return;
	}
	if(scene === 'editor'){
		await waitForDebugEvent(page, logs, 'level editor opened', 15000);
		await waitForDebugEvent(page, logs, 'level editor layout', 15000);
		return;
	}
	await waitForDebugEvent(page, logs, 'main menu layout', 15000);
	await page.keyboard.press('F');
	await waitForDebugEvent(page, logs, `loaded tmx level=${options.level}`, 20000);
	await expectVisibleGameplay(page);
}

async function httpStatus(url) {
	try {
		const response = await fetch(url, { redirect: 'follow' });
		return { ok: response.ok, status: response.status };
	}
	catch(error) {
		return { ok: false, status: 0, error: error.message };
	}
}

function withQuery(baseUrl, parameters) {
	const url = new URL(baseUrl);
	for(const [key, value] of Object.entries(parameters))
		url.searchParams.set(key, value);
	return url.toString();
}

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

async function expectVisibleGameplay(page) {
	for(let i = 0; i < 20; i++){
		const screenshot = await page.screenshot();
		if(screenshotHasNonBlackPixels(screenshot))
			return;
		await page.waitForTimeout(250);
	}
	throw new Error('Gameplay did not render visible pixels.');
}

async function getDebugEvents(page) {
	return await page.evaluate(() => window.__epicBallDebugEvents || []);
}

async function measureAnimationFrames(page, durationMs) {
	return await page.evaluate(duration => new Promise(resolve => {
		const deltas = [];
		let count = 0;
		let last = performance.now();
		const start = last;
		function tick(now) {
			count += 1;
			deltas.push(now - last);
			last = now;
			if(now - start >= duration)
				resolve({ count, durationMs: now - start, deltas });
			else
				requestAnimationFrame(tick);
		}
		requestAnimationFrame(tick);
	}), durationMs);
}

async function getMemory(page) {
	return await page.evaluate(() => {
		if(!performance.memory)
			return null;
		return {
			usedJSHeapSize: performance.memory.usedJSHeapSize,
			totalJSHeapSize: performance.memory.totalJSHeapSize,
			jsHeapSizeLimit: performance.memory.jsHeapSizeLimit
		};
	});
}

function summarizeFrames(deltas) {
	if(deltas.length === 0)
		return { average: 0, p95: 0, worst: 0 };
	const sorted = [...deltas].sort((a, b) => a - b);
	const sum = deltas.reduce((total, value) => total + value, 0);
	return {
		average: sum / deltas.length,
		p95: sorted[Math.min(sorted.length - 1, Math.floor(sorted.length * 0.95))],
		worst: sorted[sorted.length - 1]
	};
}

function screenshotHasNonBlackPixels(buffer) {
	const png = PNG.sync.read(buffer);
	const stride = Math.max(1, Math.floor(Math.sqrt((png.width * png.height) / 20000)));
	let visiblePixels = 0;
	let samples = 0;
	for(let y = 0; y < png.height; y += stride){
		for(let x = 0; x < png.width; x += stride){
			const index = (png.width * y + x) << 2;
			if(png.data[index + 3] === 0)
				continue;
			samples++;
			if(png.data[index] > 12 || png.data[index + 1] > 12 || png.data[index + 2] > 12)
				visiblePixels++;
		}
	}
	return visiblePixels > Math.max(50, samples * 0.02);
}

async function baseReport(baseUrl, page, details) {
	const scriptSource = await page.locator('script[src*="html.nocache.js"]').first().getAttribute('src').catch(() => null);
	const cacheToken = scriptSource && scriptSource.includes('?') ? scriptSource.substring(scriptSource.indexOf('?') + 1) : null;
	return {
		createdAt: new Date().toISOString(),
		url: baseUrl,
		branch: gitValue(['branch', '--show-current']),
		commit: gitValue(['rev-parse', '--short=12', 'HEAD']),
		viewport: options.viewport,
		scriptSource,
		cacheToken,
		...details
	};
}

function gitValue(args) {
	try {
		return execFileSync('git', args, { cwd: rootDir, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim();
	}
	catch(error) {
		return '';
	}
}

function assertNoBrowserErrors(errors, logs) {
	if(errors.length > 0)
		throw new Error(`Browser errors:\n${errors.join('\n')}\n\nConsole logs:\n${logs.join('\n')}`);
}

async function ensureParent(filePath) {
	await mkdir(path.dirname(filePath), { recursive: true });
}

async function writeJson(filePath, data) {
	await ensureParent(filePath);
	await writeFile(filePath, `${JSON.stringify(data, null, 2)}\n`, 'utf8');
}

function elapsedMs(startNanos) {
	return Number(process.hrtime.bigint() - startNanos) / 1_000_000;
}

function round(value) {
	return Number(value.toFixed(3));
}

function relativeToRoot(filePath) {
	return path.relative(rootDir, filePath);
}
