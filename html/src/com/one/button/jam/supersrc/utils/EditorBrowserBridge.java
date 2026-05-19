package utils;

public final class EditorBrowserBridge {
	private static PanHandler panHandler;

	private EditorBrowserBridge(){
	}

	public interface PanHandler {
		void onBrowserPanStart(int screenX, int screenY);
		void onBrowserPanMove(int screenX, int screenY);
		void onBrowserPanEnd(int screenX, int screenY);
	}

	public static void setEditorPanHandler(PanHandler handler){
		panHandler = handler;
	}

	private static void browserPanStart(int screenX, int screenY){
		if(panHandler != null)
			panHandler.onBrowserPanStart(screenX, screenY);
	}

	private static void browserPanMove(int screenX, int screenY){
		if(panHandler != null)
			panHandler.onBrowserPanMove(screenX, screenY);
	}

	private static void browserPanEnd(int screenX, int screenY){
		if(panHandler != null)
			panHandler.onBrowserPanEnd(screenX, screenY);
	}

	public static native void setEditorShortcutsActive(boolean active) /*-{
		var windowRef = $wnd;
		windowRef.__epicBallEditorShortcutsActive = active;
		if(!active){
			windowRef.__epicBallEditorPointerDrag = false;
			windowRef.__epicBallEditorPointerPanSource = null;
		}
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
			return isEditorCanvasTarget(event) || !!windowRef.__epicBallEditorPointerDrag;
		};
		var isSecondaryButtonEvent = function(event) {
			return event.button > 0 || (event.buttons & 2) !== 0 || (event.buttons & 4) !== 0;
		};
		var preventEditorBrowserGesture = function(event) {
			if(event.preventDefault)
				event.preventDefault();
		};
		var consumeEditorBrowserGesture = function(event) {
			preventEditorBrowserGesture(event);
			if(event.stopPropagation)
				event.stopPropagation();
		};
		var eventScreenX = function(event) {
			return Math.round(event.clientX || 0);
		};
		var eventScreenY = function(event) {
			return Math.round(event.clientY || 0);
		};
		var activePanSource = function(source) {
			return windowRef.__epicBallEditorPointerDrag
					&& windowRef.__epicBallEditorPointerPanSource === source;
		};
		var beginPan = function(event, source) {
			if(!isEditorCanvasTarget(event) || !isSecondaryButtonEvent(event))
				return false;
			if(!windowRef.__epicBallEditorPointerDrag){
				windowRef.__epicBallEditorPointerDrag = true;
				windowRef.__epicBallEditorPointerPanSource = source;
				@utils.EditorBrowserBridge::browserPanStart(II)(eventScreenX(event), eventScreenY(event));
			}
			consumeEditorBrowserGesture(event);
			return true;
		};
		var movePan = function(event, source) {
			if(activePanSource(source)){
				@utils.EditorBrowserBridge::browserPanMove(II)(eventScreenX(event), eventScreenY(event));
				consumeEditorBrowserGesture(event);
				return true;
			}
			if(isEditorCanvasEvent(event) && isSecondaryButtonEvent(event)){
				consumeEditorBrowserGesture(event);
				return true;
			}
			return false;
		};
		var endPan = function(event, source) {
			if(activePanSource(source)){
				@utils.EditorBrowserBridge::browserPanEnd(II)(eventScreenX(event), eventScreenY(event));
				windowRef.__epicBallEditorPointerDrag = false;
				windowRef.__epicBallEditorPointerPanSource = null;
				consumeEditorBrowserGesture(event);
				return true;
			}
			if(isEditorCanvasEvent(event) && isSecondaryButtonEvent(event)){
				consumeEditorBrowserGesture(event);
				return true;
			}
			return false;
		};
		var cancelPan = function(event, source) {
			if(activePanSource(source)){
				@utils.EditorBrowserBridge::browserPanEnd(II)(eventScreenX(event), eventScreenY(event));
				windowRef.__epicBallEditorPointerDrag = false;
				windowRef.__epicBallEditorPointerPanSource = null;
				consumeEditorBrowserGesture(event);
				return true;
			}
			return false;
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
			if(isEditorCanvasEvent(event)){
				if(windowRef.__epicBallEditorPointerDrag)
					consumeEditorBrowserGesture(event);
				else
					preventEditorBrowserGesture(event);
			}
		}, {capture: true, passive: false});
		$doc.addEventListener("auxclick", function(event) {
			if(isEditorCanvasEvent(event))
				preventEditorBrowserGesture(event);
		}, {capture: true, passive: false});
		$doc.addEventListener("pointerdown", function(event) {
			beginPan(event, "pointer");
		}, {capture: true, passive: false});
		windowRef.addEventListener("pointermove", function(event) {
			movePan(event, "pointer");
		}, {capture: true, passive: false});
		windowRef.addEventListener("pointerup", function(event) {
			endPan(event, "pointer");
		}, {capture: true, passive: false});
		windowRef.addEventListener("pointercancel", function(event) {
			cancelPan(event, "pointer");
		}, {capture: true, passive: false});
		$doc.addEventListener("mousedown", function(event) {
			beginPan(event, "mouse");
		}, {capture: true, passive: false});
		windowRef.addEventListener("mousemove", function(event) {
			movePan(event, "mouse");
		}, {capture: true, passive: false});
		windowRef.addEventListener("mouseup", function(event) {
			endPan(event, "mouse");
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
