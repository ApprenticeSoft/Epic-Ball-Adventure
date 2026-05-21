import { spawnSync } from 'node:child_process';
import path from 'node:path';

const rootDir = path.resolve(new URL('..', import.meta.url).pathname);
const args = new Set(process.argv.slice(2));
const knownArgs = new Set(['--help', '-h', '--skip-upload-signing', '--skip-live', '--skip-web-transition', '--require-android-device']);

if(args.has('--help') || args.has('-h')) {
	console.log(`Usage: npm run preflight:play-store -- [options]

Runs the Google Play release-candidate gate.

Options:
  --skip-upload-signing   Run source-side checks without requiring upload signing.
  --skip-live             Skip the live https://ball.marcvidal.ca privacy URL gate.
  --skip-web-transition   Skip the Playwright web transition suite.
  --require-android-device
                          Install and launch the game on one connected Android device.
`);
	process.exit(0);
}

for(const arg of args) {
	if(!knownArgs.has(arg)) {
		console.error(`Unknown option: ${arg}`);
		console.error('Run npm run preflight:play-store -- --help for usage.');
		process.exit(2);
	}
}

const skipUploadSigning = args.has('--skip-upload-signing');
const skipLive = args.has('--skip-live');
const skipWebTransition = args.has('--skip-web-transition');
const requireAndroidDevice = args.has('--require-android-device');
const releaseApkPath = 'android/build/outputs/apk/release/android-release.apk';

if(requireAndroidDevice && skipUploadSigning) {
	console.error('--require-android-device requires upload signing. Use npm run verify:android-device for a debug APK smoke run.');
	process.exit(2);
}

const steps = [
	{
		name: 'Build and test core release artifacts',
		command: './gradlew',
		args: [':core:test', ':desktop:compileJava', ':html:dist', ':android:assembleDebug', ':android:bundleRelease']
	},
	{
		name: 'Export Play Store metadata from source listing',
		command: 'npm',
		args: ['run', 'export:play-store-metadata']
	},
	{
		name: 'Verify local Play Store readiness',
		command: 'npm',
		args: ['run', 'verify:play-store-ready']
	}
];

if(!skipLive) {
	steps.push({
		name: 'Verify live Play Store privacy URL',
		command: 'npm',
		args: ['run', 'verify:play-store-live']
	});
}

if(!skipWebTransition) {
	steps.push({
		name: 'Run web transition suite',
		command: 'npm',
		args: ['run', 'test:web-transition']
	});
}

if(!skipUploadSigning) {
	steps.push({
		name: 'Verify upload-signed release bundle',
		command: './gradlew',
		args: [':android:verifyPlayStoreRelease']
	});
}

if(requireAndroidDevice) {
	steps.push({
		name: 'Build upload-signed release APK for Android smoke run',
		command: './gradlew',
		args: [':android:assembleRelease']
	});
	steps.push({
		name: 'Verify Android release APK smoke run',
		command: 'npm',
		args: ['run', 'verify:android-device', '--', '--apk', releaseApkPath]
	});
}

const evidenceEnv = {};
if(!skipUploadSigning)
	evidenceEnv.EPIC_BALL_REQUIRE_UPLOAD_SIGNING_EVIDENCE = '1';
if(requireAndroidDevice) {
	evidenceEnv.EPIC_BALL_REQUIRE_ANDROID_DEVICE_EVIDENCE = '1';
	evidenceEnv.EPIC_BALL_EXPECT_ANDROID_DEVICE_APK = releaseApkPath;
}

steps.push({
	name: 'Export release evidence manifest',
	command: 'npm',
	args: ['run', 'export:play-store-evidence'],
	env: evidenceEnv
});

console.log('Google Play preflight');
console.log(skipUploadSigning
	? 'Mode: source-side gate. Upload signing is intentionally skipped.'
	: 'Mode: upload-candidate gate. Upload signing is required.');
if(skipLive)
	console.log('Live privacy URL gate is skipped.');
if(skipWebTransition)
	console.log('Web transition suite is skipped.');
if(requireAndroidDevice)
	console.log('Android device smoke gate is required.');

const startedAt = Date.now();
let failedStep = null;

for(const [index, step] of steps.entries()) {
	const label = `${index + 1}/${steps.length}`;
	console.log(`\n[${label}] ${step.name}`);
	console.log(`$ ${formatCommand(step.command, step.args)}`);

	const result = spawnSync(step.command, step.args, {
		cwd: rootDir,
		stdio: 'inherit',
		env: { ...process.env, ...step.env }
	});

	if(result.error) {
		failedStep = `${step.name}: ${result.error.message}`;
		break;
	}
	if(result.status !== 0) {
		failedStep = `${step.name}: exited with status ${result.status}`;
		break;
	}
}

const duration = Math.round((Date.now() - startedAt) / 1000);
console.log(`\nPreflight duration: ${duration}s`);

if(failedStep) {
	console.error(`FAILED ${failedStep}`);
	if(!skipUploadSigning)
		console.error('If the failure is upload signing, create android/signing.properties with npm run create:upload-keystore or provide the EPIC_BALL_UPLOAD_* environment variables.');
	process.exit(1);
}

console.log('PASS automated Play Store preflight gates completed.');
console.log('Release evidence: build/play-store-release-evidence.json');
console.log(requireAndroidDevice
	? 'Manual before production promotion: upload the AAB in Play Console and complete App content forms from docs/PLAY_CONSOLE_APP_CONTENT.md.'
	: 'Manual before production promotion: upload the AAB in Play Console, complete App content forms from docs/PLAY_CONSOLE_APP_CONTENT.md, and install the release on a physical Android device.');

function formatCommand(command, commandArgs){
	return [command, ...commandArgs.map(shellQuote)].join(' ');
}

function shellQuote(value){
	if(/^[A-Za-z0-9_./:=@+-]+$/.test(value))
		return value;
	return `'${value.replaceAll("'", "'\\''")}'`;
}
