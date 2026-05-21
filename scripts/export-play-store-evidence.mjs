import { spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { mkdir, readdir, readFile, stat, writeFile } from 'node:fs/promises';
import path from 'node:path';

const rootDir = path.resolve(new URL('..', import.meta.url).pathname);
const outputPath = path.join(rootDir, 'build/play-store-release-evidence.json');
const buildGradle = await readText('android/build.gradle');

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
