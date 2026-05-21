package utils;

public final class LevelProgression {
	private LevelProgression(){
	}

	public static boolean hasNextLevel(int currentLevel, int maxLevel){
		return currentLevel < maxLevel;
	}

	public static int nextLevel(int currentLevel, int maxLevel){
		if(!hasNextLevel(currentLevel, maxLevel))
			return currentLevel;
		return currentLevel + 1;
	}

	public static int clampLevel(int level, int maxLevel){
		int safeMax = Math.max(1, maxLevel);
		if(level < 1)
			return 1;
		if(level > safeMax)
			return safeMax;
		return level;
	}

	public static boolean transitionComplete(float elapsed, float duration){
		return elapsed >= duration;
	}

	public static float transitionProgress(float elapsed, float duration){
		if(duration <= 0)
			return 1f;
		float progress = elapsed / duration;
		if(progress < 0)
			return 0f;
		if(progress > 1)
			return 1f;
		return progress;
	}
}
