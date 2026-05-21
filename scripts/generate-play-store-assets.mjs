import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { chromium, devices } from '@playwright/test';
import { PNG } from 'pngjs';

const rootDir = path.resolve(new URL('..', import.meta.url).pathname);
const distDir = path.join(rootDir, 'html/build/dist');
const outputDir = path.join(rootDir, 'docs/play-store-assets');
const phoneDir = path.join(outputDir, 'phone-screenshots');
const port = Number(process.env.EPIC_BALL_ASSET_PORT || 8092);
const baseUrl = `http://127.0.0.1:${port}`;

const phoneViewport = { width: 1080, height: 1920 };
const featureViewport = { width: 1024, height: 500 };

const appIcon = {
	file: 'app-icon.png',
	alt: 'Epic Ball Adventure app icon with a bright rolling ball on a pink platform.'
};

const phoneScenes = [
	{
		file: '01-level-1-momentum.png',
		alt: 'The ball rolls through the first bright minimal platforming level.',
		path: '/?ballDebug=1&ballStartLevel=1',
		waitFor: 'GameScreen construct end level=1',
		delayMs: 1200
	},
	{
		file: '02-level-2-timing.png',
		alt: 'The ball approaches a timing challenge with ramps and platforms.',
		path: '/?ballDebug=1&ballStartLevel=2',
		waitFor: 'GameScreen construct end level=2',
		delayMs: 1400
	},
	{
		file: '03-level-3-water.png',
		alt: 'The ball moves through a water level with visible bubbles and foam.',
		path: '/?ballDebug=1&ballStartLevel=3&ballDebugWaterBubbles=1',
		waitFor: 'water bubble probe count=14',
		delayMs: 500
	},
	{
		file: '04-level-5-pulleys.png',
		alt: 'A later level shows pulleys and physics objects around the ball.',
		path: '/?ballDebug=1&ballStartLevel=5',
		waitFor: 'GameScreen construct end level=5',
		delayMs: 1400
	}
];

const featureGraphic = {
	file: 'feature-graphic.png',
	alt: 'Epic Ball Adventure gameplay with a rolling ball, bright platforms, and water effects.',
	path: '/?ballDebug=1&ballStartLevel=3&ballDebugWaterBubbles=1',
	waitFor: 'water bubble probe count=14',
	delayMs: 500
};

if(!existsSync(path.join(distDir, 'index.html'))) {
	throw new Error('Missing html/build/dist/index.html. Run ./gradlew :html:dist before generating Play Store assets.');
}

await mkdir(phoneDir, { recursive: true });

const server = await startServer();
let browser;
try {
	browser = await chromium.launch();
	for(const scene of phoneScenes)
		await captureScene(browser, scene, phoneViewport, path.join(phoneDir, scene.file), true);
	await captureScene(browser, featureGraphic, featureViewport, path.join(outputDir, featureGraphic.file), false);
	await writeManifest();
	await validateAssets();
}
finally {
	if(browser)
		await browser.close();
	server.kill();
}

async function startServer(){
	const child = spawn('python3', ['-m', 'http.server', String(port), '--bind', '127.0.0.1', '--directory', distDir], {
		stdio: ['ignore', 'pipe', 'pipe']
	});
	let stderr = '';
	child.stderr.on('data', chunk => {
		stderr += chunk.toString();
	});
	for(let attempt = 0; attempt < 100; attempt++) {
		if(child.exitCode != null)
			throw new Error(`Asset server exited early with code ${child.exitCode}: ${stderr}`);
		try {
			const response = await fetch(baseUrl);
			if(response.ok)
				return child;
		}
		catch {
			// Server not ready yet.
		}
		await new Promise(resolve => setTimeout(resolve, 100));
	}
	child.kill();
	throw new Error(`Timed out waiting for asset server on ${baseUrl}`);
}

async function captureScene(browser, scene, viewport, outputPath, mobile){
	const context = await browser.newContext({
		viewport,
		deviceScaleFactor: 1,
		isMobile: mobile,
		hasTouch: mobile,
		userAgent: mobile ? devices['Pixel 7'].userAgent : undefined
	});
	const page = await context.newPage();
	const logs = [];
	page.on('console', message => logs.push(message.text()));
	try {
		await page.goto(`${baseUrl}${scene.path}`);
		await waitForDebugEvent(page, logs, 'main menu layout', 10000);
		await startGame(page, mobile);
		await waitForDebugEvent(page, logs, scene.waitFor, 15000);
		await page.waitForTimeout(scene.delayMs);
		const screenshot = await page.screenshot({ fullPage: false, omitBackground: false });
		await writeRgbPng(screenshot, outputPath);
		const dimensions = await pngDimensions(outputPath);
		if(dimensions.width !== viewport.width || dimensions.height !== viewport.height)
			throw new Error(`${scene.file} expected ${viewport.width}x${viewport.height}, got ${dimensions.width}x${dimensions.height}`);
		console.log(`Wrote ${path.relative(rootDir, outputPath)} ${dimensions.width}x${dimensions.height}`);
	}
	finally {
		await context.close();
	}
}

async function startGame(page, mobile){
	if(mobile) {
		await page.touchscreen.tap(phoneViewport.width / 2, phoneViewport.height / 2);
		return;
	}
	await page.keyboard.press('F');
}

async function waitForDebugEvent(page, logs, needle, timeoutMs){
	const deadline = Date.now() + timeoutMs;
	while(Date.now() < deadline) {
		const debugEvents = await page.evaluate(() => window.__epicBallDebugEvents || []);
		if(debugEvents.some(line => line.includes(needle)) || logs.some(line => line.includes(needle)))
			return;
		await page.waitForTimeout(100);
	}
	const debugEvents = await page.evaluate(() => window.__epicBallDebugEvents || []);
	throw new Error(`Timed out waiting for "${needle}". Debug events:\n${debugEvents.join('\n')}\n\nConsole logs:\n${logs.join('\n')}`);
}

async function writeRgbPng(sourceBuffer, outputPath){
	const png = PNG.sync.read(sourceBuffer);
	for(let y = 0; y < png.height; y++) {
		for(let x = 0; x < png.width; x++) {
			const index = (png.width * y + x) << 2;
			png.data[index + 3] = 255;
		}
	}
	const encoded = PNG.sync.write(png, {
		colorType: 2,
		inputColorType: 6,
		inputHasAlpha: true
	});
	await writeFile(outputPath, encoded);
}

async function pngDimensions(filePath){
	const buffer = await readFile(filePath);
	const png = PNG.sync.read(buffer);
	return {
		width: png.width,
		height: png.height,
		colorType: png.colorType,
		bytes: buffer.length
	};
}

async function validateAssets(){
	const appIconPath = path.join(outputDir, appIcon.file);
	const appIconDimensions = await pngDimensions(appIconPath);
	if(appIconDimensions.colorType !== 6)
		throw new Error(`${path.relative(rootDir, appIconPath)} must be 32-bit RGBA PNG, got colorType ${appIconDimensions.colorType}`);
	if(appIconDimensions.width !== 512 || appIconDimensions.height !== 512)
		throw new Error(`${path.relative(rootDir, appIconPath)} must be 512x512`);
	if(appIconDimensions.bytes > 1024 * 1024)
		throw new Error(`${path.relative(rootDir, appIconPath)} must be smaller than 1024KB`);

	const featurePath = path.join(outputDir, featureGraphic.file);
	const featureDimensions = await pngDimensions(featurePath);
	validateRgbPng(featurePath, featureDimensions);
	if(featureDimensions.width !== featureViewport.width || featureDimensions.height !== featureViewport.height)
		throw new Error(`${path.relative(rootDir, featurePath)} must be ${featureViewport.width}x${featureViewport.height}`);

	for(const scene of phoneScenes) {
		const file = path.join(phoneDir, scene.file);
		const dimensions = await pngDimensions(file);
		validateRgbPng(file, dimensions);
		if(dimensions.width !== phoneViewport.width || dimensions.height !== phoneViewport.height)
			throw new Error(`${path.relative(rootDir, file)} must be ${phoneViewport.width}x${phoneViewport.height}`);
		const min = Math.min(dimensions.width, dimensions.height);
		const max = Math.max(dimensions.width, dimensions.height);
		if(min < 320 || max > 3840 || max > min * 2)
			throw new Error(`${path.relative(rootDir, file)} does not satisfy Google Play screenshot dimension bounds`);
	}
}

function validateRgbPng(file, dimensions){
	if(dimensions.colorType !== 2)
		throw new Error(`${path.relative(rootDir, file)} must be RGB/no-alpha PNG, got colorType ${dimensions.colorType}`);
}

async function writeManifest(){
	const lines = [
		'# Google Play Preview Assets',
		'',
		'Generated from the current `html/build/dist` game build:',
		'',
		'```bash',
		'npm run generate:play-store-assets',
		'```',
		'',
		'The app icon is a 32-bit PNG with alpha. Preview graphics are 24-bit RGB PNGs with no alpha channel.',
		'',
		'## App Icon',
		'',
		`- \`${appIcon.file}\` - 512 x 512`,
		`  Alt text: ${appIcon.alt}`,
		'',
		'## Feature Graphic',
		'',
		`- \`${featureGraphic.file}\` - 1024 x 500`,
		`  Alt text: ${featureGraphic.alt}`,
		'',
		'## Phone Screenshots',
		''
	];
	for(const scene of phoneScenes) {
		lines.push(`- \`phone-screenshots/${scene.file}\` - 1080 x 1920`);
		lines.push(`  Alt text: ${scene.alt}`);
	}
	lines.push('');
	await writeFile(path.join(outputDir, 'README.md'), `${lines.join('\n')}\n`);
}
