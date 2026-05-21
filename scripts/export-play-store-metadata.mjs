import { copyFile, mkdir, readdir, rm, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { readFile } from 'node:fs/promises';

const rootDir = path.resolve(new URL('..', import.meta.url).pathname);
const listingPath = path.join(rootDir, 'docs/PLAY_STORE_LISTING.md');
const buildGradlePath = path.join(rootDir, 'android/build.gradle');
const outputDir = path.join(rootDir, 'fastlane/metadata/android/en-US');
const imageDir = path.join(outputDir, 'images');
const screenshotOutputDir = path.join(imageDir, 'phoneScreenshots');

const listing = await readFile(listingPath, 'utf8');
const buildGradle = await readFile(buildGradlePath, 'utf8');
const versionCode = numberAfter(buildGradle, /versionCode\s*=\s*(\d+)/);
if(versionCode == null)
	throw new Error('Could not read Android versionCode from android/build.gradle');

const title = productDetail(listing, 'App name');
const shortDescription = firstContentLine(section(listing, 'Short Description'));
const fullDescription = section(listing, 'Full Description');
const releaseNotes = section(listing, 'Release Notes');

validateText(title, 'title', 30);
validateText(shortDescription, 'short description', 80);
validateText(fullDescription, 'full description', 4000);
validateText(releaseNotes, 'release notes', 500);

await rm(outputDir, { recursive: true, force: true });
await mkdir(path.join(outputDir, 'changelogs'), { recursive: true });
await mkdir(screenshotOutputDir, { recursive: true });

await writeText('title.txt', title);
await writeText('short_description.txt', shortDescription);
await writeText('full_description.txt', fullDescription);
await writeText(path.join('changelogs', `${versionCode}.txt`), releaseNotes);

await copyAsset('docs/play-store-assets/app-icon.png', path.join('images', 'icon.png'));
await copyAsset('docs/play-store-assets/feature-graphic.png', path.join('images', 'featureGraphic.png'));

const screenshotDir = path.join(rootDir, 'docs/play-store-assets/phone-screenshots');
const screenshotFiles = (await readdir(screenshotDir))
	.filter(file => file.endsWith('.png'))
	.sort();
if(screenshotFiles.length < 3)
	throw new Error(`Expected at least 3 phone screenshots, found ${screenshotFiles.length}`);
for(const file of screenshotFiles)
	await copyAsset(path.join('docs/play-store-assets/phone-screenshots', file), path.join('images', 'phoneScreenshots', file));

console.log(`Exported Play Store metadata for versionCode ${versionCode} to fastlane/metadata/android/en-US`);

async function writeText(relativePath, text){
	await writeFile(path.join(outputDir, relativePath), `${text.trim()}\n`);
}

async function copyAsset(sourceRelativePath, targetRelativePath){
	await copyFile(path.join(rootDir, sourceRelativePath), path.join(outputDir, targetRelativePath));
}

function productDetail(markdown, label){
	const match = markdown.match(new RegExp(`^- ${escapeRegExp(label)}: (.+)$`, 'm'));
	if(!match)
		throw new Error(`Missing Product Details entry: ${label}`);
	return match[1].trim();
}

function firstContentLine(text){
	return text.split('\n')
		.map(line => line.trim())
		.find(line => line && !line.startsWith('Character count:')) || '';
}

function section(markdown, heading){
	const start = markdown.indexOf(`## ${heading}`);
	if(start < 0)
		throw new Error(`Missing listing section: ${heading}`);
	const contentStart = markdown.indexOf('\n', start) + 1;
	const next = markdown.indexOf('\n## ', contentStart);
	return markdown.substring(contentStart, next < 0 ? markdown.length : next).trim();
}

function validateText(text, label, maxLength){
	if(!text)
		throw new Error(`Play Store ${label} is empty`);
	if(text.length > maxLength)
		throw new Error(`Play Store ${label} is ${text.length} chars; max is ${maxLength}`);
}

function numberAfter(text, pattern){
	const match = text.match(pattern);
	return match ? Number(match[1]) : null;
}

function escapeRegExp(value){
	return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
