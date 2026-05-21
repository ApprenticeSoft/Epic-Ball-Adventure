import { existsSync } from 'node:fs';
import { readdir, readFile } from 'node:fs/promises';
import path from 'node:path';
import { PNG } from 'pngjs';

const rootDir = path.resolve(new URL('..', import.meta.url).pathname);

const checks = [];

try {
	await checkStoreAssets();
	await checkListingCopy();
	await checkPrivacyPolicy();
	await checkAndroidConfig();
}
catch(error) {
	fail(error.message || String(error));
}

if(checks.some(check => !check.ok)) {
	for(const check of checks)
		console[check.ok ? 'log' : 'error'](`${check.ok ? 'PASS' : 'FAIL'} ${check.message}`);
	process.exit(1);
}

for(const check of checks)
	console.log(`PASS ${check.message}`);

function pass(message){
	checks.push({ ok: true, message });
}

function fail(message){
	checks.push({ ok: false, message });
}

function failureCount(){
	return checks.filter(check => !check.ok).length;
}

function passIfNoNewFailures(startFailures, message){
	if(failureCount() === startFailures)
		pass(message);
}

async function checkStoreAssets(){
	let startFailures = failureCount();
	const appIcon = await readPng('docs/play-store-assets/app-icon.png');
	expectPng(appIcon, { width: 512, height: 512, colorType: 6, maxBytes: 1024 * 1024 });
	expectOpaque(appIcon);
	passIfNoNewFailures(startFailures, 'Play app icon is 512x512 RGBA, opaque, and under 1024KB');

	startFailures = failureCount();
	const featureGraphic = await readPng('docs/play-store-assets/feature-graphic.png');
	expectPng(featureGraphic, { width: 1024, height: 500, colorType: 2 });
	passIfNoNewFailures(startFailures, 'feature graphic is 1024x500 RGB/no-alpha PNG');

	startFailures = failureCount();
	const screenshotDir = path.join(rootDir, 'docs/play-store-assets/phone-screenshots');
	const screenshotFiles = (await readdir(screenshotDir))
		.filter(file => file.endsWith('.png'))
		.sort();
	if(screenshotFiles.length < 3)
		fail(`expected at least 3 phone screenshots, found ${screenshotFiles.length}`);
	for(const file of screenshotFiles) {
		const screenshot = await readPng(path.join('docs/play-store-assets/phone-screenshots', file));
		expectPng(screenshot, { width: 1080, height: 1920, colorType: 2 });
		const min = Math.min(screenshot.width, screenshot.height);
		const max = Math.max(screenshot.width, screenshot.height);
		if(min < 320 || max > 3840 || max > min * 2)
			fail(`${screenshot.relativePath} fails Google Play screenshot dimension bounds`);
	}
	passIfNoNewFailures(startFailures, `phone screenshots are ${screenshotFiles.length} portrait 1080x1920 RGB/no-alpha PNGs`);

	startFailures = failureCount();
	const densities = [
		['mdpi', 48],
		['hdpi', 72],
		['xhdpi', 96],
		['xxhdpi', 144],
		['xxxhdpi', 192]
	];
	for(const [density, size] of densities) {
		const icon = await readPng(`android/res/drawable-${density}/ic_launcher.png`);
		expectPng(icon, { width: size, height: size, colorType: 6 });
	}
	passIfNoNewFailures(startFailures, 'Android launcher icons are present for mdpi through xxxhdpi');

	startFailures = failureCount();
	const manifest = await readText('docs/play-store-assets/README.md');
	for(const required of [
		'app-icon.png',
		'feature-graphic.png',
		'phone-screenshots/01-level-1-momentum.png',
		'phone-screenshots/02-level-2-timing.png',
		'phone-screenshots/03-level-3-water.png',
		'phone-screenshots/04-level-5-pulleys.png',
		'Alt text:'
	]) {
		if(!manifest.includes(required))
			fail(`asset README is missing ${required}`);
	}
	passIfNoNewFailures(startFailures, 'asset manifest lists store assets and alt text');
}

async function checkListingCopy(){
	const listing = await readText('docs/PLAY_STORE_LISTING.md');
	const shortDescription = section(listing, 'Short Description').split('\n')
		.map(line => line.trim())
		.find(line => line && !line.startsWith('Character count:'));
	if(!shortDescription)
		fail('listing short description is missing');
	else if(shortDescription.length > 80)
		fail(`listing short description is ${shortDescription.length} chars; max is 80`);
	else
		pass(`listing short description is ${shortDescription.length}/80 chars`);

	const declaredCount = listing.match(/Character count:\s*(\d+)\s*\/\s*80/);
	if(shortDescription && (!declaredCount || Number(declaredCount[1]) !== shortDescription.length))
		fail('listing short description character count is stale');
	else
		pass('listing short description character count is current');

	const fullDescription = section(listing, 'Full Description');
	if(fullDescription.length > 4000)
		fail(`listing full description is ${fullDescription.length} chars; max is 4000`);
	else
		pass(`listing full description is ${fullDescription.length}/4000 chars`);

	const startFailures = failureCount();
	for(const required of [
		'No ads, no accounts, no analytics, and no personal data collection.',
		'Data collected: No user data collected.',
		'Data shared: No user data shared.',
		'Privacy policy: https://ball.marcvidal.ca/privacy.html'
	]) {
		if(!listing.includes(required))
			fail(`listing is missing required release statement: ${required}`);
	}
	passIfNoNewFailures(startFailures, 'listing includes privacy and data-safety release statements');
}

async function checkPrivacyPolicy(){
	let startFailures = failureCount();
	const sourcePolicy = await readText('docs/PRIVACY_POLICY.md');
	const webPolicy = await readText('html/webapp/privacy.html');
	for(const required of [
		'Epic Ball Adventure Privacy Policy',
		'does not collect, transmit, sell, or share personal data',
		'Privacy contact:'
	]) {
		if(!sourcePolicy.includes(required))
			fail(`source privacy policy is missing: ${required}`);
		if(!webPolicy.includes(required))
			fail(`web privacy page is missing: ${required}`);
	}
	passIfNoNewFailures(startFailures, 'source and web privacy policy contain required statements');

	startFailures = failureCount();
	const distPolicyPath = path.join(rootDir, 'html/build/dist/privacy.html');
	if(!existsSync(distPolicyPath)) {
		fail('html/build/dist/privacy.html is missing; run ./gradlew :html:dist before final Play verification');
		return;
	}
	const distPolicy = await readFile(distPolicyPath, 'utf8');
	if(!distPolicy.includes('Epic Ball Adventure Privacy Policy'))
		fail('built privacy page is missing expected title');
	else
		passIfNoNewFailures(startFailures, 'built web distribution includes privacy.html');
}

async function checkAndroidConfig(){
	let startFailures = failureCount();
	const manifest = await readText('android/AndroidManifest.xml');
	for(const forbidden of ['<uses-permission', 'android.permission.INTERNET']) {
		if(manifest.includes(forbidden))
			fail(`Android manifest unexpectedly includes ${forbidden}`);
	}
	for(const required of [
		'android:allowBackup="false"',
		'android:fullBackupContent="false"',
		'android:appCategory="game"',
		'android:isGame="true"',
		'android:exported="true"',
		'android:icon="@drawable/ic_launcher"'
	]) {
		if(!manifest.includes(required))
			fail(`Android manifest is missing ${required}`);
	}
	passIfNoNewFailures(startFailures, 'Android manifest has game metadata, disabled backup, and no declared permissions');

	startFailures = failureCount();
	const buildGradle = await readText('android/build.gradle');
	const targetSdk = numberAfter(buildGradle, /targetSdk\s*=\s*(\d+)/);
	if(targetSdk == null || targetSdk < 35)
		fail(`targetSdk must be at least 35, found ${targetSdk ?? 'missing'}`);
	else
		pass(`targetSdk is ${targetSdk}`);

	for(const required of [
		'versionCode = 1',
		'versionName = "1.0.0"',
		'tasks.register("verifyPlayStoreRelease")'
	]) {
		if(!buildGradle.includes(required))
			fail(`Android build config is missing ${required}`);
	}
	passIfNoNewFailures(startFailures, 'Android release build metadata is present');
}

async function readPng(relativePath){
	const buffer = await readFile(path.join(rootDir, relativePath));
	const png = PNG.sync.read(buffer);
	return {
		relativePath,
		width: png.width,
		height: png.height,
		colorType: png.colorType,
		bytes: buffer.length,
		data: png.data
	};
}

async function readText(relativePath){
	return await readFile(path.join(rootDir, relativePath), 'utf8');
}

function expectPng(png, expected){
	if(png.width !== expected.width || png.height !== expected.height)
		fail(`${png.relativePath} expected ${expected.width}x${expected.height}, got ${png.width}x${png.height}`);
	if(png.colorType !== expected.colorType)
		fail(`${png.relativePath} expected PNG colorType ${expected.colorType}, got ${png.colorType}`);
	if(expected.maxBytes && png.bytes > expected.maxBytes)
		fail(`${png.relativePath} is ${png.bytes} bytes; max is ${expected.maxBytes}`);
}

function expectOpaque(png){
	for(let index = 3; index < png.data.length; index += 4) {
		if(png.data[index] !== 255) {
			fail(`${png.relativePath} contains transparent pixels`);
			return;
		}
	}
}

function section(markdown, heading){
	const start = markdown.indexOf(`## ${heading}`);
	if(start < 0)
		return '';
	const contentStart = markdown.indexOf('\n', start) + 1;
	const next = markdown.indexOf('\n## ', contentStart);
	return markdown.substring(contentStart, next < 0 ? markdown.length : next).trim();
}

function numberAfter(text, pattern){
	const match = text.match(pattern);
	return match ? Number(match[1]) : null;
}
