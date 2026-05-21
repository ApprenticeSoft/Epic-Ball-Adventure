import { createHash } from 'node:crypto';
import { existsSync } from 'node:fs';
import { mkdir, readFile, stat, writeFile } from 'node:fs/promises';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import { PNG } from 'pngjs';

const rootDir = path.resolve(new URL('..', import.meta.url).pathname);
const options = parseArgs(process.argv.slice(2));
const defaultPackageId = 'com.apprenticesoft.epicballadventure';

if(options.help) {
	console.log(`Usage: npm run verify:android-device -- [options]

Installs the Android APK on one connected Android device, launches the game,
captures a screenshot, and writes smoke-test evidence under build/.

Options:
  --apk <path>       APK to install. Default: android/build/outputs/apk/debug/android-debug.apk
  --package <id>     Android package id. Default: com.apprenticesoft.epicballadventure
  --skip-version-check
                    Do not compare installed version metadata with android/build.gradle.
  --serial <serial>  Device serial when more than one device is connected.
`);
	process.exit(0);
}

const packageId = options.packageId || defaultPackageId;
const apkPath = path.resolve(rootDir, options.apk || 'android/build/outputs/apk/debug/android-debug.apk');
const evidencePath = path.join(rootDir, 'build/android-device-smoke-evidence.json');
const screenshotPath = path.join(rootDir, 'build/android-device-smoke.png');

if(!existsSync(apkPath))
	fail(`${path.relative(rootDir, apkPath)} is missing; run ./gradlew :android:assembleDebug first`);

const adbPrefix = selectDevice(options.serial);
const apkEvidence = await fileEvidence(apkPath);
const device = {
	serial: adbPrefix.length === 2 ? adbPrefix[1] : activeDeviceSerial(),
	manufacturer: shellTrim(['shell', 'getprop', 'ro.product.manufacturer']),
	model: shellTrim(['shell', 'getprop', 'ro.product.model']),
	androidRelease: shellTrim(['shell', 'getprop', 'ro.build.version.release']),
	sdk: shellTrim(['shell', 'getprop', 'ro.build.version.sdk']),
	abi: shellTrim(['shell', 'getprop', 'ro.product.cpu.abi'])
};

runAdb(['install', '-r', apkPath], { inherit: true });
runAdb(['shell', 'monkey', '-p', packageId, '-c', 'android.intent.category.LAUNCHER', '1'], { inherit: true });
await delay(3000);

const pid = shellTrim(['shell', 'pidof', packageId]);
if(!pid)
	fail(`Installed ${packageId}, but it was not running after launch`);

const packageDump = shellTrim(['shell', 'dumpsys', 'package', packageId]);
const screenshot = runAdb(['exec-out', 'screencap', '-p'], { buffer: true });
const image = screenshotEvidenceStats(screenshot.stdout);
const installed = {
	versionCode: textAfter(packageDump, /versionCode=(\d+)/),
	versionName: textAfter(packageDump, /versionName=([^\s]+)/)
};
const expectedVersion = await expectedProjectVersion();
if(!options.skipVersionCheck && packageId === defaultPackageId)
	assertInstalledVersion(installed, expectedVersion);

await mkdir(path.dirname(evidencePath), { recursive: true });
await writeFile(screenshotPath, screenshot.stdout);
const screenshotEvidence = await fileEvidence(screenshotPath);

const evidence = {
	generatedAt: new Date().toISOString(),
	packageId,
	apk: apkEvidence,
	device,
	installed,
	expectedVersion,
	launch: {
		pid
	},
	screenshot: {
		...screenshotEvidence,
		image
	}
};

await writeFile(evidencePath, `${JSON.stringify(evidence, null, 2)}\n`);

console.log(`PASS launched ${packageId} on ${device.manufacturer} ${device.model} (${device.serial})`);
console.log(`Evidence: ${path.relative(rootDir, evidencePath)}`);
console.log(`Screenshot: ${path.relative(rootDir, screenshotPath)}`);

function parseArgs(args){
	const parsed = {};
	for(let index = 0; index < args.length; index++) {
		const arg = args[index];
		if(arg === '--help' || arg === '-h') {
			parsed.help = true;
		}
		else if(arg === '--apk') {
			parsed.apk = requiredValue(args, ++index, arg);
		}
		else if(arg === '--package') {
			parsed.packageId = requiredValue(args, ++index, arg);
		}
		else if(arg === '--skip-version-check') {
			parsed.skipVersionCheck = true;
		}
		else if(arg === '--serial') {
			parsed.serial = requiredValue(args, ++index, arg);
		}
		else {
			fail(`Unknown option: ${arg}`);
		}
	}
	return parsed;
}

function requiredValue(args, index, option){
	const value = args[index];
	if(!value || value.startsWith('--'))
		fail(`${option} requires a value`);
	return value;
}

function selectDevice(serial){
	const version = spawnSync('adb', ['version'], { encoding: 'utf8' });
	if(version.error?.code === 'ENOENT')
		fail('adb is not installed or not on PATH');
	if(version.status !== 0)
		fail(`adb version failed: ${version.stderr || version.stdout}`);

	const devices = adbDevices();
	if(serial) {
		const match = devices.find(device => device.serial === serial);
		if(!match)
			fail(`adb device ${serial} is not connected and authorized`);
		return ['-s', serial];
	}
	if(devices.length === 0)
		fail('No authorized Android device is connected. Connect one physical device with USB debugging enabled.');
	if(devices.length > 1)
		fail(`Multiple Android devices are connected; rerun with --serial. Devices: ${devices.map(device => device.serial).join(', ')}`);
	return ['-s', devices[0].serial];
}

function adbDevices(){
	const result = spawnSync('adb', ['devices'], { encoding: 'utf8' });
	if(result.status !== 0)
		fail(`adb devices failed: ${result.stderr || result.stdout}`);
	return result.stdout.split('\n')
		.map(line => line.trim())
		.filter(line => line && !line.startsWith('List of devices'))
		.map(line => {
			const [serial, state] = line.split(/\s+/);
			return { serial, state };
		})
		.filter(device => device.state === 'device');
}

function activeDeviceSerial(){
	return adbPrefix.length === 2 ? adbPrefix[1] : '';
}

function runAdb(args, options = {}){
	const result = spawnSync('adb', [...adbPrefix, ...args], {
		cwd: rootDir,
		encoding: options.buffer ? null : 'utf8',
		stdio: options.inherit ? 'inherit' : 'pipe',
		maxBuffer: 1024 * 1024 * 16
	});
	if(result.error)
		fail(result.error.message);
	if(result.status !== 0)
		fail(`adb ${args.join(' ')} failed${result.stderr ? `: ${result.stderr}` : ''}`);
	return result;
}

function shellTrim(args){
	return String(runAdb(args).stdout || '').trim();
}

async function fileEvidence(absolutePath){
	const fileStat = await stat(absolutePath);
	const buffer = await readFile(absolutePath);
	return {
		path: path.relative(rootDir, absolutePath),
		bytes: fileStat.size,
		sha256: createHash('sha256').update(buffer).digest('hex')
	};
}

function screenshotEvidenceStats(buffer){
	let png;
	try {
		png = PNG.sync.read(buffer);
	}
	catch(error) {
		fail(`Captured screenshot is not a valid PNG: ${error.message}`);
	}

	let visiblePixels = 0;
	let transparentPixels = 0;
	let brightPixels = 0;
	let colorSpread = 0;
	for(let index = 0; index < png.data.length; index += 4) {
		const red = png.data[index];
		const green = png.data[index + 1];
		const blue = png.data[index + 2];
		const alpha = png.data[index + 3];
		if(alpha === 0) {
			transparentPixels++;
			continue;
		}
		const brightness = red + green + blue;
		if(brightness > 24)
			visiblePixels++;
		if(brightness > 180)
			brightPixels++;
		colorSpread += Math.max(red, green, blue) - Math.min(red, green, blue);
	}

	const totalPixels = png.width * png.height;
	const visibleRatio = totalPixels === 0 ? 0 : visiblePixels / totalPixels;
	const brightRatio = totalPixels === 0 ? 0 : brightPixels / totalPixels;
	const averageColorSpread = totalPixels === 0 ? 0 : colorSpread / totalPixels;
	if(png.width <= 0 || png.height <= 0)
		fail('Captured screenshot has invalid dimensions');
	if(visibleRatio < 0.02)
		fail(`Captured screenshot appears blank or black: visibleRatio=${visibleRatio.toFixed(4)}`);
	if(brightRatio < 0.001 && averageColorSpread < 1)
		fail('Captured screenshot has no meaningful bright or colored pixels');

	return {
		width: png.width,
		height: png.height,
		visiblePixels,
		visibleRatio: Number(visibleRatio.toFixed(4)),
		transparentPixels,
		brightPixels,
		brightRatio: Number(brightRatio.toFixed(4)),
		averageColorSpread: Number(averageColorSpread.toFixed(2))
	};
}

async function expectedProjectVersion(){
	const buildGradle = await readFile(path.join(rootDir, 'android/build.gradle'), 'utf8');
	return {
		versionCode: textAfter(buildGradle, /versionCode\s*=\s*(\d+)/),
		versionName: textAfter(buildGradle, /versionName\s*=\s*"([^"]+)"/)
	};
}

function assertInstalledVersion(installed, expected){
	if(expected.versionCode && installed.versionCode !== expected.versionCode)
		fail(`Installed versionCode is ${installed.versionCode || 'missing'}; expected ${expected.versionCode}`);
	if(expected.versionName && installed.versionName !== expected.versionName)
		fail(`Installed versionName is ${installed.versionName || 'missing'}; expected ${expected.versionName}`);
}

function textAfter(text, pattern){
	const match = text.match(pattern);
	return match ? match[1] : '';
}

function delay(ms){
	return new Promise(resolve => setTimeout(resolve, ms));
}

function fail(message){
	console.error(`FAIL ${message}`);
	process.exit(1);
}
