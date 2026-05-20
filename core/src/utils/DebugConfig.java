package utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;

public final class DebugConfig {
	public static boolean transitionLogs;
	public static boolean autoAdvanceLevels;
	public static boolean showRestartOverlay;
	public static boolean startEditor;
	public static boolean waterBubbleProbe;
	public static boolean waterTuningOverlay;
	public static boolean editorInvalidPlayProbe;
	public static boolean adaptiveBubbleThrottle = true;
	public static float autoAdvanceDelay = 0.35f;
	public static float waterBubbleDensityMultiplier = 1f;
	public static float waterBubbleSizeMultiplier = 1f;
	public static float waterBubbleLifetimeMultiplier = 1f;
	public static float waterFoamAmount = 1f;
	public static int startLevel = 1;
	public static int editorLoadLevel = 0;

	private DebugConfig(){
	}

	public static boolean isEnabled(){
		return transitionLogs || autoAdvanceLevels || showRestartOverlay || startEditor
				|| waterBubbleProbe || waterTuningOverlay || editorInvalidPlayProbe || editorLoadLevel > 0;
	}

	public static float waterBubbleDensityScale(){
		return clampFinite(waterBubbleDensityMultiplier, 0f, 4f, 1f);
	}

	public static float waterBubbleSizeScale(){
		return clampFinite(waterBubbleSizeMultiplier, 0.35f, 2.5f, 1f);
	}

	public static float waterBubbleLifetimeScale(){
		return clampFinite(waterBubbleLifetimeMultiplier, 0.35f, 2.5f, 1f);
	}

	public static float waterFoamAmountScale(){
		return clampFinite(waterFoamAmount, 0f, 3f, 1f);
	}

	public static void setWaterBubbleDensityMultiplier(float value){
		waterBubbleDensityMultiplier = clampFinite(value, 0f, 4f, 1f);
	}

	public static void setWaterBubbleSizeMultiplier(float value){
		waterBubbleSizeMultiplier = clampFinite(value, 0.35f, 2.5f, 1f);
	}

	public static void setWaterBubbleLifetimeMultiplier(float value){
		waterBubbleLifetimeMultiplier = clampFinite(value, 0.35f, 2.5f, 1f);
	}

	public static void setWaterFoamAmount(float value){
		waterFoamAmount = clampFinite(value, 0f, 3f, 1f);
	}

	private static float clampFinite(float value, float min, float max, float fallback){
		if(Float.isNaN(value) || Float.isInfinite(value))
			return fallback;
		return MathUtils.clamp(value, min, max);
	}

	public static void log(String message){
		if(!isEnabled())
			return;
		if(Gdx.app != null)
			Gdx.app.log("EpicBallDebug", message);
		System.out.println("[EpicBallDebug] " + message);
		DebugBridge.log(message);
	}

	public static void reset(){
		transitionLogs = false;
		autoAdvanceLevels = false;
		showRestartOverlay = false;
		startEditor = false;
		waterBubbleProbe = false;
		waterTuningOverlay = false;
		editorInvalidPlayProbe = false;
		adaptiveBubbleThrottle = true;
		autoAdvanceDelay = 0.35f;
		waterBubbleDensityMultiplier = 1f;
		waterBubbleSizeMultiplier = 1f;
		waterBubbleLifetimeMultiplier = 1f;
		waterFoamAmount = 1f;
		startLevel = 1;
		editorLoadLevel = 0;
	}
}
