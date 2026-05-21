import https from 'node:https';

const rootUrl = 'https://ball.marcvidal.ca/';
const privacyUrl = 'https://ball.marcvidal.ca/privacy.html';
const checks = [];

try {
	await checkRoot();
	await checkPrivacyPolicy();
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

async function checkRoot(){
	const response = await request(rootUrl, { method: 'HEAD' });
	if(response.statusCode !== 200) {
		fail(`${rootUrl} returned HTTP ${response.statusCode}`);
		return;
	}
	pass(`${rootUrl} is reachable`);
}

async function checkPrivacyPolicy(){
	const startFailures = failureCount();
	const response = await request(privacyUrl);
	if(response.statusCode !== 200) {
		fail(`${privacyUrl} returned HTTP ${response.statusCode}; redeploy html/build/dist before using this URL in Play Console`);
		return;
	}

	const contentType = response.headers['content-type'] || '';
	if(!contentType.toLowerCase().includes('text/html'))
		fail(`${privacyUrl} must serve an HTML privacy policy, got ${contentType || 'no content-type'}`);

	for(const required of [
		'Epic Ball Adventure Privacy Policy',
		'does not collect, transmit, sell, or share personal data',
		'Privacy contact:'
	]) {
		if(!response.body.includes(required))
			fail(`${privacyUrl} is missing required policy text: ${required}`);
	}

	if(failureCount() === startFailures)
		pass(`${privacyUrl} is an active public HTML privacy policy`);
}

function failureCount(){
	return checks.filter(check => !check.ok).length;
}

async function request(url, options = {}, redirects = 0){
	return await new Promise((resolve, reject) => {
		const req = https.request(url, { method: options.method || 'GET', timeout: 10000 }, response => {
			const location = response.headers.location;
			if(location && response.statusCode >= 300 && response.statusCode < 400 && redirects < 5) {
				response.resume();
				resolve(request(new URL(location, url).toString(), options, redirects + 1));
				return;
			}

			let body = '';
			response.setEncoding('utf8');
			response.on('data', chunk => {
				body += chunk;
			});
			response.on('end', () => {
				resolve({
					statusCode: response.statusCode,
					headers: response.headers,
					body
				});
			});
		});

		req.on('timeout', () => {
			req.destroy(new Error(`Timed out requesting ${url}`));
		});
		req.on('error', reject);
		req.end();
	});
}
