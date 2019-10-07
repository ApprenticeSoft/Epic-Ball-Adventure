varying vec4 v_color;				//varying = partagé par le vertex shader et le fragment shader
varying vec2 v_texCoord0;

uniform sampler2D u_sampler2D;		//sampler2D = prend la couleur d'un pixel d'une texture

void main(){
	vec4 color = texture2D(u_sampler2D, v_texCoord0) * v_color;
	color.rgb = 1 - color.rgb;
	gl_FragColor = color;
}