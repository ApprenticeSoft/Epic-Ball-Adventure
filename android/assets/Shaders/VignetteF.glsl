//precision mediump float;
varying vec4 v_color;				//varying = partagé par le vertex shader et le fragment shader
varying vec2 v_texCoord0;

uniform vec2 u_resolution;
uniform float u_PosX, u_PosY;
uniform sampler2D u_sampler2D;		//sampler2D = prend la couleur d'un pixel d'une texture

//const float outerRadius = 25.0, innerRadius = 1.0, intensity = .8;
const float intensity = 1.0;
uniform float outerRadius, innerRadius;

void main(){
	vec4 color = texture2D(u_sampler2D, v_texCoord0) * v_color;
	
	vec2 relativePosition = gl_FragCoord.xy / u_resolution - vec2(u_PosX, u_PosY);
	relativePosition.x *= u_resolution.x / u_resolution.y;	//Si on veut que l'effet de vignette soit un rond
	float len = length(relativePosition);
	float vignette = smoothstep(outerRadius, innerRadius, len);
	color.rgb = mix(color.rgb, color.rgb * vignette, intensity);
	
	gl_FragColor = color;  //Classic passthrough shader
}