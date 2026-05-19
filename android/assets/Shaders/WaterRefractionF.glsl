#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_localCoord;

uniform sampler2D u_sceneTexture;
uniform vec2 u_resolution;
uniform float u_time;
uniform float u_calmDistortionPixels;
uniform float u_waveDistortionPixels;
uniform float u_surfaceRippleStrength;
uniform float u_tintStrength;
uniform float u_waveStrength;

void main(){
	vec2 screenUv = vec2(gl_FragCoord.x / u_resolution.x, gl_FragCoord.y / u_resolution.y);
	float localX = clamp(v_localCoord.x, 0.0, 1.0);
	float localY = clamp(v_localCoord.y, 0.0, 1.0);
	float depth = clamp(1.0 - localY, 0.0, 1.0);
	float surfaceGuard = smoothstep(0.015, 0.12, depth);
	float surfaceBend = (1.0 - smoothstep(0.05, 0.88, depth)) * surfaceGuard;
	float deepBend = smoothstep(0.0, 0.42, depth);
	float sideFade = smoothstep(0.0, 0.12, localX) * smoothstep(0.0, 0.12, 1.0 - localX);
	float waveStrength = clamp(u_waveStrength, 0.0, 1.0);

	float surfaceWave = sin(localX * 24.0 + u_time * (2.0 + waveStrength * 1.1));
	float impactWave = sin(localX * 38.0 - u_time * (2.5 + waveStrength * 1.5));
	float crossWave = sin((localX * 12.0 + localY * 5.0) - u_time * 1.15);
	float shimmer = sin((localX - localY * 0.32) * 44.0 + u_time * 2.4);
	float waveMix = surfaceWave * 0.52 + impactWave * 0.32 * waveStrength + crossWave * 0.12 + shimmer * 0.015;
	float distortionPixels = u_calmDistortionPixels + u_waveDistortionPixels * waveStrength;

	float distortionMask = 0.32 + surfaceBend * u_surfaceRippleStrength + deepBend * 0.18;
	float verticalWave = crossWave * 0.58 + shimmer * 0.18 + surfaceWave * 0.18 * waveStrength;
	vec2 offsetPixels = vec2(
		waveMix * distortionPixels * distortionMask * sideFade,
		-abs(verticalWave) * distortionPixels * (0.10 + surfaceBend * 0.24 + deepBend * 0.16)
	);

	vec2 sampleUv = screenUv + offsetPixels / u_resolution;
	sampleUv.x = clamp(sampleUv.x, 0.001, 0.999);
	sampleUv.y = clamp(sampleUv.y, 0.001, max(screenUv.y, 0.001));
	float xFilterPixels = 0.55 + waveStrength * 0.7 + (1.0 - sideFade) * 1.45;
	vec2 xStep = vec2(xFilterPixels / u_resolution.x, 0.0);
	vec4 refracted = texture2D(u_sceneTexture, sampleUv) * 0.40;
	refracted += texture2D(u_sceneTexture, vec2(clamp(sampleUv.x - xStep.x, 0.001, 0.999), sampleUv.y)) * 0.24;
	refracted += texture2D(u_sceneTexture, vec2(clamp(sampleUv.x + xStep.x, 0.001, 0.999), sampleUv.y)) * 0.24;
	refracted += texture2D(u_sceneTexture, vec2(clamp(sampleUv.x - xStep.x * 2.0, 0.001, 0.999), sampleUv.y)) * 0.06;
	refracted += texture2D(u_sceneTexture, vec2(clamp(sampleUv.x + xStep.x * 2.0, 0.001, 0.999), sampleUv.y)) * 0.06;
	float tint = clamp(u_tintStrength + v_color.a * 0.18, 0.0, 0.58);
	vec3 color = mix(refracted.rgb, v_color.rgb, tint);
	float caustic = (sin(localX * 95.0 + localY * 28.0 - u_time * 2.4) * 0.5 + 0.5)
		* (0.06 + waveStrength * 0.1) * surfaceBend * (0.35 + sideFade * 0.65);
	color += vec3(0.04, 0.07, 0.08) * surfaceBend * v_color.a;
	color += vec3(0.06, 0.09, 0.08) * caustic;

	gl_FragColor = vec4(color, 1.0);
}
