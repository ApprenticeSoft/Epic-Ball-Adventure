package utils;

public final class DebugBridge {
	private DebugBridge(){}

	public static native void log(String message) /*-{
		var windowRef = $wnd;
		if(!windowRef.__epicBallDebugEvents)
			windowRef.__epicBallDebugEvents = [];
		windowRef.__epicBallDebugEvents.push(message);
		if(windowRef.console && windowRef.console.log)
			windowRef.console.log("[EpicBallDebug] " + message);
	}-*/;

	public static native void setCurrentLevel(int level) /*-{
		var windowRef = $wnd;
		if(!windowRef.__epicBallState)
			windowRef.__epicBallState = {};
		windowRef.__epicBallState.currentLevel = level;
	}-*/;
}
