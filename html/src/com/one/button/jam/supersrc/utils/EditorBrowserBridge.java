package utils;

public final class EditorBrowserBridge {
	private EditorBrowserBridge(){
	}

	public static native void setEditorShortcutsActive(boolean active) /*-{
		var windowRef = $wnd;
		windowRef.__epicBallEditorShortcutsActive = active;
		if(windowRef.__epicBallEditorShortcutsInstalled)
			return;
		windowRef.__epicBallEditorShortcutsInstalled = true;
		var isEditorCanvasTarget = function(event) {
			if(!windowRef.__epicBallEditorShortcutsActive)
				return false;
			var target = event.target;
			return !!(target && (target.tagName === "CANVAS" || target.id === "embed-html"));
		};
		var isEditorCanvasEvent = function(event) {
			return isEditorCanvasTarget(event);
		};
		var preventEditorBrowserGesture = function(event) {
			if(event.preventDefault)
				event.preventDefault();
		};
		$doc.addEventListener("keydown", function(event) {
			if(!windowRef.__epicBallEditorShortcutsActive)
				return;
			if(!(event.ctrlKey || event.metaKey))
				return;
			var key = (event.key || "").toLowerCase();
			if(key === "z" || key === "y")
				event.preventDefault();
		}, true);
		$doc.addEventListener("contextmenu", function(event) {
			if(isEditorCanvasEvent(event))
				preventEditorBrowserGesture(event);
		}, {capture: true, passive: false});
		$doc.addEventListener("auxclick", function(event) {
			if(isEditorCanvasEvent(event))
				preventEditorBrowserGesture(event);
		}, {capture: true, passive: false});
		$doc.addEventListener("dragstart", function(event) {
			if(isEditorCanvasEvent(event))
				preventEditorBrowserGesture(event);
		}, {capture: true, passive: false});
		$doc.addEventListener("selectstart", function(event) {
			if(isEditorCanvasEvent(event))
				preventEditorBrowserGesture(event);
		}, {capture: true, passive: false});
		$doc.addEventListener("wheel", function(event) {
			if(isEditorCanvasEvent(event))
				preventEditorBrowserGesture(event);
		}, {capture: true, passive: false});
	}-*/;
}
