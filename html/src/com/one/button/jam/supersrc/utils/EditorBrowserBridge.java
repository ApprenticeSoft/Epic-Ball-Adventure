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
		var isEditorCanvasEvent = function(event) {
			if(!windowRef.__epicBallEditorShortcutsActive)
				return false;
			var target = event.target;
			return !!(target && (target.tagName === "CANVAS" || target.id === "embed-html"));
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
				event.preventDefault();
		}, true);
		$doc.addEventListener("auxclick", function(event) {
			if(isEditorCanvasEvent(event))
				event.preventDefault();
		}, true);
		$doc.addEventListener("mousedown", function(event) {
			if(isEditorCanvasEvent(event) && event.button > 0)
				event.preventDefault();
		}, true);
		$doc.addEventListener("mouseup", function(event) {
			if(isEditorCanvasEvent(event) && event.button > 0)
				event.preventDefault();
		}, true);
		$doc.addEventListener("wheel", function(event) {
			if(isEditorCanvasEvent(event))
				event.preventDefault();
		}, {capture: true, passive: false});
	}-*/;
}
