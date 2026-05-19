package utils;

public final class EditorBrowserBridge {
	private EditorBrowserBridge(){
	}

	public interface PanHandler {
		void onBrowserPanStart(int screenX, int screenY);
		void onBrowserPanMove(int screenX, int screenY);
		void onBrowserPanEnd(int screenX, int screenY);
	}

	public static void setEditorShortcutsActive(boolean active){
	}

	public static void setEditorPanHandler(PanHandler handler){
	}
}
