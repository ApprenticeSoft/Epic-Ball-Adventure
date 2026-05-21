import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, writeFileSync } from 'node:fs';
import path from 'node:path';

const rootDir = path.resolve(new URL('..', import.meta.url).pathname);
const defaultStoreFile = path.join(rootDir, 'android/keystores/upload.jks');
const defaultPropertiesFile = path.join(rootDir, 'android/signing.properties');
const storeFile = path.resolve(process.env.EPIC_BALL_UPLOAD_STORE_FILE || defaultStoreFile);
const propertiesFile = path.resolve(process.env.EPIC_BALL_SIGNING_PROPERTIES_FILE || defaultPropertiesFile);
const storePassword = requiredSecret('EPIC_BALL_UPLOAD_STORE_PASSWORD');
const keyPassword = process.env.EPIC_BALL_UPLOAD_KEY_PASSWORD || storePassword;
const keyAlias = process.env.EPIC_BALL_UPLOAD_KEY_ALIAS || 'epic-ball-upload';
const overwrite = process.env.EPIC_BALL_OVERWRITE_UPLOAD_KEYSTORE === '1';

if(storePassword.length < 6)
	throw new Error('EPIC_BALL_UPLOAD_STORE_PASSWORD must be at least 6 characters for keytool.');
if(keyPassword.length < 6)
	throw new Error('EPIC_BALL_UPLOAD_KEY_PASSWORD must be at least 6 characters for keytool.');
if(existsSync(storeFile) && !overwrite)
	throw new Error(`${path.relative(rootDir, storeFile)} already exists. Set EPIC_BALL_OVERWRITE_UPLOAD_KEYSTORE=1 to replace it.`);

mkdirSync(path.dirname(storeFile), { recursive: true });
mkdirSync(path.dirname(propertiesFile), { recursive: true });

const result = spawnSync('keytool', [
	'-genkeypair',
	'-v',
	'-keystore', storeFile,
	'-storetype', 'JKS',
	'-storepass', storePassword,
	'-keypass', keyPassword,
	'-alias', keyAlias,
	'-keyalg', 'RSA',
	'-keysize', '4096',
	'-validity', '10000',
	'-dname', 'CN=Epic Ball Adventure Upload,O=ApprenticeSoft,C=CA'
], {
	encoding: 'utf8',
	stdio: ['ignore', 'pipe', 'pipe']
});

if(result.status !== 0)
	throw new Error(`keytool failed:\n${result.stderr || result.stdout}`);

writeFileSync(propertiesFile, [
	'# Local upload-signing config for Epic Ball Adventure. Do not commit this file.',
	`EPIC_BALL_UPLOAD_STORE_FILE=${javaPropertiesValue(storeFile)}`,
	`EPIC_BALL_UPLOAD_STORE_PASSWORD=${javaPropertiesValue(storePassword)}`,
	`EPIC_BALL_UPLOAD_KEY_ALIAS=${javaPropertiesValue(keyAlias)}`,
	`EPIC_BALL_UPLOAD_KEY_PASSWORD=${javaPropertiesValue(keyPassword)}`,
	''
].join('\n'), 'utf8');

console.log(`Created upload keystore at ${path.relative(rootDir, storeFile)}`);
console.log(`Wrote local signing properties to ${path.relative(rootDir, propertiesFile)}`);
console.log('Back up the keystore and password outside the repository before uploading a release.');
console.log('Next: ./gradlew :android:verifyPlayStoreRelease');

function requiredSecret(name){
	const value = process.env[name];
	if(!value || !value.trim())
		throw new Error(`${name} is required. Set it in the environment so it is not stored in shell history.`);
	return value;
}

function javaPropertiesValue(value){
	return String(value).replace(/[\\\n\r:=#!]/g, character => {
		switch(character) {
		case '\\': return '\\\\';
		case '\n': return '\\n';
		case '\r': return '\\r';
		default: return `\\${character}`;
		}
	});
}
