package utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public final class WaterRefractionRenderer {
	private static final float CALM_DISTORTION_PIXELS = 5.75f;
	private static final float WAVE_DISTORTION_PIXELS = 25f;
	private static final float SURFACE_RIPPLE_STRENGTH = 1.18f;
	private static final float TINT_STRENGTH = 0.18f;

	private ShaderProgram shader;
	private FrameBuffer sceneBuffer;
	private int bufferWidth;
	private int bufferHeight;
	private float time;
	private boolean disabled;
	private boolean captureActive;
	private boolean capturedCurrentFrame;

	public WaterRefractionRenderer(){
		ShaderProgram.pedantic = false;
		try{
			String vertexShader = Gdx.files.internal("Shaders/WaterRefractionV.glsl").readString();
			String fragmentShader = Gdx.files.internal("Shaders/WaterRefractionF.glsl").readString();
			shader = new ShaderProgram(vertexShader, fragmentShader);
			if(!shader.isCompiled())
				disable("shader compile failed: " + shader.getLog(), null);
		}
		catch(RuntimeException exception){
			disable("shader load failed", exception);
		}
	}

	public void update(float delta){
		time += Math.max(0f, delta);
	}

	public void resize(int width, int height){
		if(disabled || shader == null || !shader.isCompiled())
			return;
		width = Math.max(1, width);
		height = Math.max(1, height);
		if(sceneBuffer != null && bufferWidth == width && bufferHeight == height)
			return;
		disposeBuffer();
		try{
			sceneBuffer = new FrameBuffer(Format.RGB565, width, height, false);
			Texture texture = sceneBuffer.getColorBufferTexture();
			texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
			texture.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
			bufferWidth = width;
			bufferHeight = height;
		}
		catch(RuntimeException exception){
			disable("framebuffer unavailable", exception);
		}
	}

	public boolean beginCapture(Color clearColor){
		if(!isReady())
			return false;
		capturedCurrentFrame = false;
		try{
			sceneBuffer.begin();
			Color color = clearColor == null ? Color.BLACK : clearColor;
			Gdx.gl.glClearColor(color.r, color.g, color.b, 1f);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			captureActive = true;
			return true;
		}
		catch(RuntimeException exception){
			captureActive = false;
			disable("capture failed", exception);
			return false;
		}
	}

	public void endCapture(){
		if(!captureActive)
			return;
		captureActive = false;
		sceneBuffer.end();
		capturedCurrentFrame = true;
	}

	public boolean beginWaterPass(PolygonSpriteBatch batch, float waveStrength){
		if(!isReady() || !capturedCurrentFrame)
			return false;
		sceneBuffer.getColorBufferTexture().bind(1);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		batch.setShader(shader);
		batch.begin();
		shader.setUniformi("u_sceneTexture", 1);
		shader.setUniformf("u_resolution", bufferWidth, bufferHeight);
		shader.setUniformf("u_time", time);
		shader.setUniformf("u_calmDistortionPixels", calculateDistortionPixels(0f));
		shader.setUniformf("u_waveDistortionPixels", WAVE_DISTORTION_PIXELS);
		shader.setUniformf("u_surfaceRippleStrength", SURFACE_RIPPLE_STRENGTH);
		shader.setUniformf("u_tintStrength", TINT_STRENGTH);
		shader.setUniformf("u_waveStrength", Math.min(Math.max(waveStrength, 0f), 1f));
		return true;
	}

	static float calculateDistortionPixels(float waveStrength){
		float strength = Math.min(Math.max(waveStrength, 0f), 1f);
		return CALM_DISTORTION_PIXELS + WAVE_DISTORTION_PIXELS * strength;
	}

	public void endWaterPass(PolygonSpriteBatch batch){
		batch.end();
		batch.setShader(null);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
	}

	public boolean isReady(){
		return !disabled && shader != null && shader.isCompiled() && sceneBuffer != null;
	}

	public void dispose(){
		disposeBuffer();
		if(shader != null)
			shader.dispose();
		shader = null;
	}

	private void disposeBuffer(){
		if(sceneBuffer != null)
			sceneBuffer.dispose();
		sceneBuffer = null;
		bufferWidth = 0;
		bufferHeight = 0;
	}

	private void disable(String message, RuntimeException exception){
		disabled = true;
		DebugConfig.log("water refraction disabled: " + message);
		if(exception != null)
			DebugConfig.log("water refraction error=" + exception);
		disposeBuffer();
	}
}
