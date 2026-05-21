import { createHash } from 'node:crypto';
import { existsSync } from 'node:fs';
import { mkdir, readFile, stat, writeFile } from 'node:fs/promises';
import { spawnSync } from 'node:child_process';
import path from 'node:path';

const rootDir = path.resolve(new URL('..', import.meta.url).pathname);
const options = parseArgs(process.argv.slice(2));

if(options.help) {
	console.log(`Usage: npm run verify:android-device -- [options]

Installs the Android APK on one connected Android device, launches the game,
captures a screenshot, and writes smoke-test evidence under build/.

Options:
  --apk <path>       APK to install. Default: android/build/outputs/apk/debug/android-debug.apk
  --package <id>     Android package id. Default: com.apprenticesoft.epicballadventure
  --serial <serial>  Device serial when more than one device is connected.
`);
	process.exit(0);
}

const packageId = options.packageId || 'com.apprenticesoft.epicballadventure';
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

await mkdir(path.dirname(evidencePath), { recursive: true });
await writeFile(screenshotPath, screenshot.stdout);
const screenshotEvidence = await fileEvidence(screenshotPath);

const evidence = {
	generatedAt: new Date().toISOString(),
	packageId,
	apk: apkEvidence,
	device,
	installed: {
		versionCode: textAfter(packageDump, /versionCode=(\d+)/),
		versionName: textAfter(packageDump, /versionName=([^\s]+)/)
	},
	launch: {
		pid
	},
	screenshot: screenshotEvidence
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
