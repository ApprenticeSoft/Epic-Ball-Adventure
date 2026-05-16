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
		$doc.addEventListener("keydown", function(event) {
			if(!windowRef.__epicBallEditorShortcutsActive)
				return;
			if(!(event.ctrlKey || event.metaKey))
				return;
			var key = (event.key || "").toLowerCase();
			if(key === "z" || key === "y")
				event.preventDefault();
		}, true);
	}-*/;
}
