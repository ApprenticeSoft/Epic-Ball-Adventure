varying vec4 v_color;				//varying = partagé par le vertex shader et le fragment shader
varying vec2 v_texCoord0;

uniform vec2 u_resolution;
uniform float u_PosX, u_PosY;
uniform sampler2D u_sampler2D;		//sampler2D = prend la couleur d'un pixel d'une texture

const float outerRadius = 1.5, innerRadius = .2, intensity = .8;

void main(){
	vec4 color = texture2D(u_sampler2D, v_texCoord0) * v_color;
	
	vec2 relativePosition = gl_FragCoord.xy / u_resolution - vec2(u_PosX, u_PosY);
	relativePosition.x *= u_resolution.x / u_resolution.y;	//Si on veut que l'effet de vignette soit un rond
	float len = length(relativePosition);
	float vignette = smoothstep(outerRadius, innerRadius, len);
	//color.rgb = mix(color.rgb, (color.rgb) * vignette, intensity);	
	
	vec2 uv = gl_TexCoord[0].xy;
	uv -= vec2(u_PosX, u_PosY)/2;
	
	float dist =  sqrt(dot(uv, uv));
	if(dist < len)
		color.rgb = 1 -color.rgb;
		
	
	gl_FragColor = color;  //Classic passthrough shader
}