#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_localCoord;

uniform sampler2D u_sceneTexture;
uniform vec2 u_resolution;
uniform float u_time;
uniform float u_distortionPixels;
uniform float u_surfaceRippleStrength;
uniform float u_tintStrength;

void main(){
	vec2 screenUv = vec2(gl_FragCoord.x / u_resolution.x, 1.0 - gl_FragCoord.y / u_resolution.y);
	float localX = clamp(v_localCoord.x, 0.0, 1.0);
	float localY = clamp(v_localCoord.y, 0.0, 1.0);
	float depth = clamp(1.0 - localY, 0.0, 1.0);
	float surfaceFade = 1.0 - smoothstep(0.08, 0.82, depth);

	float longWave = sin(localX * 38.0 + u_time * 2.2);
	float crossWave = sin((localX * 19.0 + localY * 11.0) - u_time * 1.45);
	float shimmer = sin((localX - localY) * 57.0 + u_time * 3.1);
	float waveMix = longWave * 0.58 + crossWave * 0.32 + shimmer * 0.1;

	vec2 offsetPixels = vec2(
		waveMix * u_distortionPixels,
		(crossWave * 0.65 + shimmer * 0.35) * u_distortionPixels * 0.42
	) * (0.35 + surfaceFade * u_surfaceRippleStrength) * clamp(v_color.a, 0.0, 1.0);

	vec4 refracted = texture2D(u_sceneTexture, clamp(screenUv + offsetPixels / u_resolution, 0.001, 0.999));
	float tint = clamp(u_tintStrength + v_color.a * 0.32, 0.0, 0.82);
	vec3 color = mix(refracted.rgb, v_color.rgb, tint);
	color += vec3(0.04, 0.07, 0.08) * surfaceFade * v_color.a;

	gl_FragColor = vec4(color, v_color.a);
}
