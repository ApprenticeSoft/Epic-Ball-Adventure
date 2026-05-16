package editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.TimeUtils;

public final class EditorFileBridge {
	private EditorFileBridge(){
	}

	public static String loadText(String fileName){
		String sanitized = sanitize(fileName);
		FileHandle local = Gdx.files.local("Levels/" + sanitized);
		if(local.exists())
			return local.readString("UTF-8");
		FileHandle internal = Gdx.files.internal("Levels/" + sanitized);
		if(internal.exists())
			return internal.readString("UTF-8");
		throw new RuntimeException("Level not found: " + sanitized);
	}

	public static void saveText(String fileName, String content){
		Gdx.files.local("Levels/" + sanitize(fileName)).writeString(content, false, "UTF-8");
	}

	public static String saveTextWithBackup(String fileName, String content){
		String sanitized = sanitize(fileName);
		String backupName = backupExisting(sanitized);
		Gdx.files.local("Levels/" + sanitized).writeString(content, false, "UTF-8");
		return backupName;
	}

	public static String sanitize(String fileName){
		String value = fileName == null || fileName.trim().length() == 0 ? "Editor Level.tmx" : fileName.trim();
		if(!value.endsWith(".tmx"))
			value += ".tmx";
		return value.replace('\\', '_').replace('/', '_');
	}

	private static String backupExisting(String sanitized){
		FileHandle local = Gdx.files.local("Levels/" + sanitized);
		if(!local.exists())
			return null;
		String baseName = sanitized.endsWith(".tmx") ? sanitized.substring(0, sanitized.length() - 4) : sanitized;
		String backupName = baseName + "." + TimeUtils.millis() + ".bak.tmx";
		Gdx.files.local("Levels/backups/" + backupName).writeString(local.readString("UTF-8"), false, "UTF-8");
		return backupName;
	}
}
