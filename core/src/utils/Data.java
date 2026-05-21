package utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class Data {

	public static Preferences prefs;
	private static final String PREFS_NAME = "Epic Ball Adventure.Progress";
	private static final String LEVEL_KEY = "Level";

	public static void Load(){
		prefs = Gdx.app.getPreferences(PREFS_NAME);

		if(!prefs.contains(LEVEL_KEY))
			setLevel(1);
	}

	public static void setLevel(int val) {
		if(prefs == null)
			return;
		prefs.putInteger(LEVEL_KEY, Math.max(1, val));
		prefs.flush();							//Mandatory to save the data
	}

	public static int getLevel() {
		return getLevel(Variables.nombreNiveaux);
	}

	public static int getLevel(int maxLevel) {
		if(prefs == null)
			return 1;
		return LevelProgression.clampLevel(prefs.getInteger(LEVEL_KEY, 1), maxLevel);
	}
}
