package editor;

import com.badlogic.gdx.Gdx;

public final class EditorFileBridge {
	private EditorFileBridge(){
	}

	public static void saveText(String fileName, String content){
		Gdx.files.local("Levels/" + sanitize(fileName)).writeString(content, false, "UTF-8");
	}

	private static String sanitize(String fileName){
		String value = fileName == null || fileName.trim().length() == 0 ? "Editor Level.tmx" : fileName.trim();
		if(!value.endsWith(".tmx"))
			value += ".tmx";
		return value.replace('\\', '_').replace('/', '_');
	}
}
