attribute vec4 a_color;			//attribute = fourni par GLSL
attribute vec3 a_position;
attribute vec2 a_texCoord0;

uniform mat4 u_projTrans;		//uniform = variable normale (comme en java). On peut modifier ces variables à partir du code java

varying vec4 v_color;			//varying = partagé par le vertex shader et le fragment shader
varying vec2 v_texCoord0;

void main(){
	v_color = a_color;
	v_texCoord0 = a_texCoord0;
	gl_Position = u_projTrans * vec4(a_position, 1.0);	//gl_Position = variable déjà incluse (pas besoin de la déclarée), on peut lui attribuer la position des vertice
}