precision mediump float;
varying vec4 v_color;				//varying = partagé par le vertex shader et le fragment shader
varying vec2 v_texCoord0;

uniform vec2 u_resolution;
uniform vec2 u_center;
uniform sampler2D u_texture;		//sampler2D = prend la couleur d'un pixel d'une texture

//const float outerRadius = 25.0, innerRadius = 1.0, intensity = .8;
const float intensity = 1.0;
uniform float outerRadius, innerRadius;

void main(){
	vec4 color = texture2D(u_texture, v_texCoord0) * v_color;
	
	vec2 relativePosition = gl_FragCoord.xy - u_center;
	float len = length(relativePosition);
	float vignette = 1.0 - smoothstep(innerRadius, outerRadius, len);
	color.rgb = mix(color.rgb, color.rgb * vignette, intensity);
	
	gl_FragColor = color;  //Classic passthrough shader
}