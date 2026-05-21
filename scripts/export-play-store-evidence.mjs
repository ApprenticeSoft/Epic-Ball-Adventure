import { spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { mkdir, readdir, readFile, stat, writeFile } from 'node:fs/promises';
import path from 'node:path';

const rootDir = path.resolve(new URL('..', import.meta.url).pathname);
const outputPath = path.join(rootDir, 'build/play-store-release-evidence.json');
const buildGradle = await readText('android/build.gradle');
const requireUploadSigningEvidence = process.env.EPIC_BALL_REQUIRE_UPLOAD_SIGNING_EVIDENCE === '1';
const requireAndroidDeviceEvidence = process.env.EPIC_BALL_REQUIRE_ANDROID_DEVICE_EVIDENCE === '1';
const expectedAndroidDeviceApk = normalizeRelativePath(process.env.EPIC_BALL_EXPECT_ANDROID_DEVICE_APK || '');

const evidence = {
	generatedAt: new Date().toISOString(),
	git: {
		branch: git(['rev-parse', '--abbrev-ref', 'HEAD']),
		commit: git(['rev-parse', 'HEAD']),
		dirty: git(['status', '--porcelain']).length > 0
	},
	android: {
		applicationId: textAfter(buildGradle, /applicationId\s*=\s*"([^"]+)"/),
		namespace: textAfter(buildGradle, /namespace\s*=\s*"([^"]+)"/),
		versionCode: numberAfter(buildGradle, /versionCode\s*=\s*(\d+)/),
		versionName: textAfter(buildGradle, /versionName\s*=\s*"([^"]+)"/),
		minSdk: numberAfter(buildGradle, /minSdk\s*=\s*(\d+)/),
		targetSdk: numberAfter(buildGradle, /targetSdk\s*=\s*(\d+)/)
	},
	artifacts: {},
	uploadSigning: {},
	androidDeviceSmoke: {},
	playMetadata: {},
	policy: {},
	live: {
		site: 'https://ball.marcvidal.ca/',
		privacyPolicy: 'https://ball.marcvidal.ca/privacy.html'
	}
};

for(const required of [
	['android.applicationId', evidence.android.applicationId],
	['android.namespace', evidence.android.namespace],
	['android.versionCode', evidence.android.versionCode],
	['android.versionName', evidence.android.versionName],
	['android.minSdk', evidence.android.minSdk],
	['android.targetSdk', evidence.android.targetSdk]
]) {
	if(required[1] == null || required[1] === '')
		throw new Error(`Could not read ${required[0]} from android/build.gradle`);
}

evidence.artifacts.releaseBundle = await fileEvidence('android/build/outputs/bundle/release/android-release.aab');
evidence.artifacts.debugApk = await fileEvidence('android/build/outputs/apk/debug/android-debug.apk');
evidence.artifacts.releaseApk = await optionalFileEvidence('android/build/outputs/apk/release/android-release.apk');
evidence.uploadSigning = await uploadSigningEvidence(requireUploadSigningEvidence);
evidence.androidDeviceSmoke = await androidDeviceSmokeEvidence([
	evidence.artifacts.debugApk,
	evidence.artifacts.releaseApk
], requireAndroidDeviceEvidence, expectedAndroidDeviceApk);

evidence.playMetadata.fastlane = await groupEvidence('fastlane/metadata/android/en-US', [
	'title.txt',
	'short_description.txt',
	'full_description.txt',
	`changelogs/${evidence.android.versionCode}.txt`,
	'images/icon.png',
	'images/featureGraphic.png',
	...await relativeFiles('fastlane/metadata/android/en-US/images/phoneScreenshots', '.png', 'fastlane/metadata/android/en-US')
]);

evidence.playMetadata.source = await groupEvidence('.', [
	'docs/PLAY_STORE_LISTING.md',
	'docs/play-store-assets/README.md',
	'docs/play-store-assets/app-icon.png',
	'docs/play-store-assets/feature-graphic.png',
	...await relativeFiles('docs/play-store-assets/phone-screenshots', '.png', '.')
]);

evidence.policy = await groupEvidence('.', [
	'docs/PRIVACY_POLICY.md',
	'html/webapp/privacy.html',
	'html/build/dist/privacy.html',
	'docs/PLAY_CONSOLE_APP_CONTENT.md'
]);

await mkdir(path.dirname(outputPath), { recursive: true });
await writeFile(outputPath, `${JSON.stringify(evidence, null, 2)}\n`);

console.log(`Exported Play Store release evidence to ${path.relative(rootDir, outputPath)}`);
console.log(`Release AAB SHA-256: ${evidence.artifacts.releaseBundle.sha256}`);

async function fileEvidence(relativePath){
	const absolutePath = path.join(rootDir, relativePath);
	const fileStat = await stat(absolutePath).catch(error => {
		if(error.code === 'ENOENT')
			throw new Error(`${relativePath} is missing; run the Play Store build gate first`);
		throw error;
	});
	const buffer = await readFile(absolutePath);
	return {
		path: relativePath,
		bytes: fileStat.size,
		sha256: createHash('sha256').update(buffer).digest('hex')
	};
}

async function optionalFileEvidence(relativePath){
	try {
		return await fileEvidence(relativePath);
	}
	catch(error) {
		if(error.message.startsWith(`${relativePath} is missing;`))
			return null;
		throw error;
	}
}

async function groupEvidence(baseRelativePath, fileRelativePaths){
	const files = {};
	for(const fileRelativePath of fileRelativePaths) {
		const combinedPath = path.posix.join(baseRelativePath, fileRelativePath);
		const normalizedPath = combinedPath === '.' ? fileRelativePath : combinedPath.replace(/^\.\//, '');
		files[fileRelativePath] = await fileEvidence(normalizedPath);
	}
	return {
		basePath: baseRelativePath,
		files
	};
}

async function uploadSigningEvidence(required){
	const propertiesPath = 'android/signing.properties';
	const propertiesText = await readText(propertiesPath).catch(error => {
		if(error.code === 'ENOENT')
			return null;
		throw error;
	});
	if(propertiesText == null) {
		if(required)
			throw new Error(`${propertiesPath} is missing; run npm run create:upload-keystore before exporting signed release evidence`);
		return {
			status: 'not-configured',
			propertiesPath
		};
	}

	const properties = parseJavaProperties(propertiesText);
	const missing = [
		'EPIC_BALL_UPLOAD_STORE_FILE',
		'EPIC_BALL_UPLOAD_STORE_PASSWORD',
		'EPIC_BALL_UPLOAD_KEY_ALIAS',
		'EPIC_BALL_UPLOAD_KEY_PASSWORD'
	].filter(name => !properties[name]);
	const storePath = normalizeRelativePath(properties.EPIC_BALL_UPLOAD_STORE_FILE || '');
	const result = {
		status: 'configured',
		propertiesPath,
		storeFile: storePath,
		keyAlias: properties.EPIC_BALL_UPLOAD_KEY_ALIAS || ''
	};
	if(missing.length > 0) {
		result.status = 'incomplete';
		result.missing = missing;
		if(required)
			throw new Error(`${propertiesPath} is missing required upload signing values: ${missing.join(', ')}`);
		return result;
	}

	const storeStat = await stat(path.join(rootDir, storePath)).catch(error => {
		if(error.code === 'ENOENT')
			return null;
		throw error;
	});
	if(storeStat == null) {
		result.status = 'missing-keystore';
		if(required)
			throw new Error(`upload keystore does not exist: ${storePath}`);
		return result;
	}

	result.certificate = uploadCertificateEvidence(properties);
	if(required && result.status !== 'configured')
		throw new Error(`upload signing evidence is ${result.status}`);
	return result;
}

function uploadCertificateEvidence(properties){
	const result = spawnSync('keytool', [
		'-list',
		'-v',
		'-keystore', properties.EPIC_BALL_UPLOAD_STORE_FILE,
		'-storepass:env', 'EPIC_BALL_UPLOAD_STORE_PASSWORD',
		'-alias', properties.EPIC_BALL_UPLOAD_KEY_ALIAS
	], {
		cwd: rootDir,
		encoding: 'utf8',
		env: {
			...process.env,
			EPIC_BALL_UPLOAD_STORE_PASSWORD: properties.EPIC_BALL_UPLOAD_STORE_PASSWORD
		},
		maxBuffer: 1024 * 1024
	});
	if(result.error)
		throw new Error(`keytool failed: ${result.error.message}`);
	if(result.status !== 0)
		throw new Error(`keytool failed while reading upload certificate: ${result.stderr || result.stdout}`);

	const output = result.stdout;
	return {
		owner: textAfter(output, /^Owner:\s*(.+)$/m),
		issuer: textAfter(output, /^Issuer:\s*(.+)$/m),
		serialNumber: textAfter(output, /^Serial number:\s*(.+)$/m),
		validity: textAfter(output, /^Valid from:\s*(.+)$/m),
		sha1: textAfter(output, /^\s*SHA1:\s*([0-9A-F:]+)$/m),
		sha256: textAfter(output, /^\s*SHA256:\s*([0-9A-F:]+)$/m),
		signatureAlgorithm: textAfter(output, /^Signature algorithm name:\s*(.+)$/m)
	};
}

async function androidDeviceSmokeEvidence(apkEvidenceList, required, expectedApkPath){
	const evidencePath = 'build/android-device-smoke-evidence.json';
	const screenshotPath = 'build/android-device-smoke.png';
	const rawEvidence = await readFile(path.join(rootDir, evidencePath), 'utf8').catch(error => {
		if(error.code === 'ENOENT')
			return null;
		throw error;
	});
	if(rawEvidence == null) {
		if(required)
			throw new Error(`${evidencePath} is missing; run npm run verify:android-device -- --apk ${expectedApkPath || 'android/build/outputs/apk/debug/android-debug.apk'} before exporting full release evidence`);
		return {
			status: 'not-recorded',
			expectedApkPath: expectedApkPath || '',
			evidencePath,
			screenshotPath
		};
	}

	let smoke;
	try {
		smoke = JSON.parse(rawEvidence);
	}
	catch(error) {
		if(required)
			throw new Error(`${evidencePath} is not valid JSON: ${error.message}`);
		return {
			status: 'invalid',
			evidenceFile: await fileEvidence(evidencePath),
			reasons: [`Invalid JSON: ${error.message}`]
		};
	}

	const reasons = [];
	const smokeFile = await fileEvidence(evidencePath);
	const screenshotFile = await fileEvidence(screenshotPath).catch(error => {
		reasons.push(error.message);
		return null;
	});
	const apkEvidence = apkEvidenceList.filter(Boolean);
	const smokeApkPath = normalizeRelativePath(smoke.apk?.path || '');
	const expectedApkEvidence = expectedApkPath ? apkEvidence.find(apk => apk.path === expectedApkPath) : null;
	const matchedApk = expectedApkEvidence
		|| apkEvidence.find(apk => apk.path === smokeApkPath)
		|| apkEvidence.find(apk => apk.sha256 === smoke.apk?.sha256);

	if(expectedApkPath && !expectedApkEvidence)
		reasons.push(`Expected Android device smoke APK is missing: ${expectedApkPath}`);
	if(expectedApkPath && smokeApkPath !== expectedApkPath)
		reasons.push(`Device smoke APK path is ${smokeApkPath || 'missing'}; expected ${expectedApkPath}`);
	if(!matchedApk)
		reasons.push('Device smoke APK does not match a current debug or release APK artifact');
	else if(smoke.apk?.sha256 !== matchedApk.sha256)
		reasons.push(`Device smoke APK SHA-256 does not match the current ${matchedApk.path}`);
	if(screenshotFile && smoke.screenshot?.sha256 !== screenshotFile.sha256)
		reasons.push('Device smoke screenshot SHA-256 does not match the current screenshot file');
	if(!smoke.device?.serial)
		reasons.push('Device smoke evidence is missing the Android device serial');
	if(!smoke.launch?.pid)
		reasons.push('Device smoke evidence is missing the launched process id');

	const result = {
		status: reasons.length === 0 ? 'recorded' : 'stale-or-invalid',
		expectedApkPath: expectedApkPath || '',
		evidenceFile: smokeFile,
		apk: smoke.apk || {},
		screenshot: screenshotFile,
		generatedAt: smoke.generatedAt || '',
		packageId: smoke.packageId || '',
		device: smoke.device || {},
		installed: smoke.installed || {},
		launch: smoke.launch || {}
	};
	if(reasons.length > 0)
		result.reasons = reasons;
	if(required && result.status !== 'recorded')
		throw new Error(`${evidencePath} is not valid for this release: ${reasons.join('; ')}`);
	return result;
}

function normalizeRelativePath(value){
	if(!value)
		return '';
	const normalized = path.isAbsolute(value)
		? path.relative(rootDir, value)
		: path.normalize(value);
	return normalized.replaceAll(path.sep, '/').replace(/^\.\//, '');
}

function parseJavaProperties(text){
	const properties = {};
	for(const rawLine of text.split(/\r?\n/)) {
		const line = rawLine.trim();
		if(!line || line.startsWith('#') || line.startsWith('!'))
			continue;
		const separator = line.search(/(?<!\\)[=:]/);
		const key = separator === -1 ? line : line.slice(0, separator);
		const value = separator === -1 ? '' : line.slice(separator + 1);
		properties[unescapeJavaProperty(key.trim())] = unescapeJavaProperty(value.trim());
	}
	return properties;
}

function unescapeJavaProperty(value){
	return value.replace(/\\([\\nrt:=#!])/g, (_, character) => {
		switch(character) {
		case 'n': return '\n';
		case 'r': return '\r';
		case 't': return '\t';
		default: return character;
		}
	});
}

async function relativeFiles(directoryRelativePath, extension, baseRelativePath){
	const files = (await readdir(path.join(rootDir, directoryRelativePath)))
		.filter(file => file.endsWith(extension))
		.sort();
	if(files.length === 0)
		throw new Error(`${directoryRelativePath} has no ${extension} files`);
	return files.map(file => path.posix.relative(baseRelativePath, path.posix.join(directoryRelativePath, file)));
}

async function readText(relativePath){
	return await readFile(path.join(rootDir, relativePath), 'utf8');
}

function git(args){
	const result = spawnSync('git', args, {
		cwd: rootDir,
		encoding: 'utf8',
		maxBuffer: 1024 * 1024
	});
	if(result.status !== 0)
		return '';
	return result.stdout.trim();
}

function numberAfter(text, pattern){
	const match = text.match(pattern);
	return match ? Number(match[1]) : null;
}

function textAfter(text, pattern){
	const match = text.match(pattern);
	return match ? match[1] : '';
}
