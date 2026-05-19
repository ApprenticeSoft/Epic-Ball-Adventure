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
	float waveStrength = clamp(u_waveStrength, 0.0, 1.0);

	float surfaceWave = sin(localX * 32.0 + u_time * (2.1 + waveStrength * 1.2));
	float impactWave = sin(localX * 54.0 - u_time * (2.8 + waveStrength * 1.8));
	float crossWave = sin((localX * 18.0 + localY * 15.0) - u_time * 1.45);
	float shimmer = sin((localX - localY) * 71.0 + u_time * 3.4);
	float waveMix = surfaceWave * 0.46 + impactWave * 0.34 * waveStrength + crossWave * 0.16 + shimmer * 0.04;
	float distortionPixels = u_calmDistortionPixels + u_waveDistortionPixels * waveStrength;

	float distortionMask = 0.32 + surfaceBend * u_surfaceRippleStrength + deepBend * 0.18;
	float verticalWave = crossWave * 0.48 + shimmer * 0.36 + surfaceWave * 0.22 * waveStrength;
	vec2 offsetPixels = vec2(
		waveMix * distortionPixels * distortionMask,
		-abs(verticalWave) * distortionPixels * (0.10 + surfaceBend * 0.24 + deepBend * 0.16)
	);

	vec2 sampleUv = screenUv + offsetPixels / u_resolution;
	sampleUv.x = clamp(sampleUv.x, 0.001, 0.999);
	sampleUv.y = clamp(sampleUv.y, 0.001, max(screenUv.y, 0.001));
	vec4 refracted = texture2D(u_sceneTexture, sampleUv);
	float tint = clamp(u_tintStrength + v_color.a * 0.18, 0.0, 0.58);
	vec3 color = mix(refracted.rgb, v_color.rgb, tint);
	float caustic = (sin(localX * 95.0 + localY * 28.0 - u_time * 2.4) * 0.5 + 0.5)
		* (0.06 + waveStrength * 0.1) * surfaceBend;
	color += vec3(0.04, 0.07, 0.08) * surfaceBend * v_color.a;
	color += vec3(0.06, 0.09, 0.08) * caustic;

	gl_FragColor = vec4(color, 1.0);
}
