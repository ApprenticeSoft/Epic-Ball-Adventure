package utils;

import com.badlogic.gdx.Gdx;

public final class DebugConfig {
	public static boolean transitionLogs;
	public static boolean autoAdvanceLevels;
	public static boolean showRestartOverlay;
	public static boolean startEditor;
	public static float autoAdvanceDelay = 0.35f;
	public static int startLevel = 1;

	private DebugConfig(){
	}

	public static boolean isEnabled(){
		return transitionLogs || autoAdvanceLevels;
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
		autoAdvanceDelay = 0.35f;
		startLevel = 1;
	}
}
