import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { PNG } from 'pngjs';

const rootDir = path.resolve(new URL('..', import.meta.url).pathname);

const outputs = [
	{ file: 'docs/play-store-assets/app-icon.png', size: 512, playStore: true },
	{ file: 'android/ic_launcher-web.png', size: 512 },
	{ file: 'android/res/drawable-mdpi/ic_launcher.png', size: 48 },
	{ file: 'android/res/drawable-hdpi/ic_launcher.png', size: 72 },
	{ file: 'android/res/drawable-xhdpi/ic_launcher.png', size: 96 },
	{ file: 'android/res/drawable-xxhdpi/ic_launcher.png', size: 144 },
	{ file: 'android/res/drawable-xxxhdpi/ic_launcher.png', size: 192 }
];

for(const output of outputs)
	await mkdir(path.dirname(path.join(rootDir, output.file)), { recursive: true });

for(const output of outputs) {
	const png = createIcon(output.size);
	const encoded = PNG.sync.write(png, {
		colorType: 6,
		inputColorType: 6,
		inputHasAlpha: true
	});
	const outputPath = path.join(rootDir, output.file);
	await writeFile(outputPath, encoded);
	console.log(`Wrote ${output.file} ${output.size}x${output.size}`);
}

await validateOutputs();

function createIcon(size){
	const png = new PNG({ width: size, height: size, colorType: 6 });
	fill(png, rgba(239, 21, 88));

	drawRect(png, 72, 334, 440, 392, rgba(250, 199, 166));
	drawRect(png, 342, 296, 386, 334, rgba(126, 113, 160));
	drawRect(png, 72, 392, 440, 406, rgba(217, 185, 154));

	const ballMask = { cx: 256, cy: 232, radius: 134 };
	drawCircle(png, ballMask.cx, ballMask.cy, ballMask.radius, rgba(82, 38, 103));
	drawCircle(png, ballMask.cx, ballMask.cy, 122, rgba(249, 142, 18));
	drawCircle(png, 302, 188, 112, rgba(255, 214, 61), ballMask);
	drawCircle(png, 224, 294, 82, rgba(248, 134, 14), ballMask);
	drawCircle(png, 330, 158, 43, rgba(92, 40, 111), ballMask);
	drawCircle(png, 312, 150, 33, rgba(255, 214, 61), ballMask);
	drawCircle(png, 212, 176, 16, rgba(255, 248, 214, 170), ballMask);
	drawCircle(png, 282, 120, 12, rgba(255, 248, 214, 115), ballMask);

	return png;
}

function fill(png, color){
	for(let y = 0; y < png.height; y++) {
		for(let x = 0; x < png.width; x++)
			setPixel(png, x, y, color);
	}
}

function drawRect(png, x0, y0, x1, y1, color){
	const scale = png.width / 512;
	const left = Math.max(0, Math.floor(x0 * scale));
	const top = Math.max(0, Math.floor(y0 * scale));
	const right = Math.min(png.width, Math.ceil(x1 * scale));
	const bottom = Math.min(png.height, Math.ceil(y1 * scale));
	for(let y = top; y < bottom; y++) {
		for(let x = left; x < right; x++)
			setPixel(png, x, y, color);
	}
}

function drawCircle(png, cx, cy, radius, color, mask){
	const scale = png.width / 512;
	const scaledCx = cx * scale;
	const scaledCy = cy * scale;
	const scaledRadius = radius * scale;
	const left = Math.max(0, Math.floor(scaledCx - scaledRadius - 2));
	const top = Math.max(0, Math.floor(scaledCy - scaledRadius - 2));
	const right = Math.min(png.width, Math.ceil(scaledCx + scaledRadius + 2));
	const bottom = Math.min(png.height, Math.ceil(scaledCy + scaledRadius + 2));
	for(let y = top; y < bottom; y++) {
		for(let x = left; x < right; x++) {
			const coverage = circleCoverage(x + 0.5, y + 0.5, scaledCx, scaledCy, scaledRadius);
			if(coverage <= 0)
				continue;
			if(mask) {
				const maskCoverage = circleCoverage(x + 0.5, y + 0.5, mask.cx * scale, mask.cy * scale, mask.radius * scale);
				if(maskCoverage <= 0)
					continue;
				blendPixel(png, x, y, color, Math.min(coverage, maskCoverage));
			}
			else {
				blendPixel(png, x, y, color, coverage);
			}
		}
	}
}

function circleCoverage(x, y, cx, cy, radius){
	const edgeDistance = radius - Math.hypot(x - cx, y - cy);
	if(edgeDistance >= 0.75)
		return 1;
	if(edgeDistance <= -0.75)
		return 0;
	return (edgeDistance + 0.75) / 1.5;
}

function blendPixel(png, x, y, color, coverage){
	const index = (png.width * y + x) << 2;
	const sourceAlpha = (color.a / 255) * coverage;
	const inverse = 1 - sourceAlpha;
	png.data[index] = Math.round(color.r * sourceAlpha + png.data[index] * inverse);
	png.data[index + 1] = Math.round(color.g * sourceAlpha + png.data[index + 1] * inverse);
	png.data[index + 2] = Math.round(color.b * sourceAlpha + png.data[index + 2] * inverse);
	png.data[index + 3] = 255;
}

function setPixel(png, x, y, color){
	const index = (png.width * y + x) << 2;
	png.data[index] = color.r;
	png.data[index + 1] = color.g;
	png.data[index + 2] = color.b;
	png.data[index + 3] = color.a;
}

function rgba(r, g, b, a = 255){
	return { r, g, b, a };
}

async function validateOutputs(){
	for(const output of outputs) {
		const outputPath = path.join(rootDir, output.file);
		const buffer = await readFile(outputPath);
		const png = PNG.sync.read(buffer);
		if(png.width !== output.size || png.height !== output.size)
			throw new Error(`${output.file} expected ${output.size}x${output.size}, got ${png.width}x${png.height}`);
		if(png.colorType !== 6)
			throw new Error(`${output.file} must be a 32-bit RGBA PNG, got colorType ${png.colorType}`);
		if(output.playStore && buffer.length > 1024 * 1024)
			throw new Error(`${output.file} must be smaller than 1024KB for Google Play`);
		for(let index = 3; index < png.data.length; index += 4) {
			if(png.data[index] !== 255)
				throw new Error(`${output.file} must use full-square opaque artwork for Google Play icon masking`);
		}
	}
}
